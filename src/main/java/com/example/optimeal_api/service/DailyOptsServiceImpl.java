package com.example.optimeal_api.service;

import com.example.optimeal_api.entity.DailyOpts;
import com.example.optimeal_api.entity.DailyOptsId;
import com.example.optimeal_api.exception.CutoffDeadlinePassedException;
import com.example.optimeal_api.exception.MealCapExceededException;
import com.example.optimeal_api.repository.DailyOptsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation of {@link DailyOptsService}.
 *
 * <p>The only mutating method, {@link #optOutFromMeal}, is {@code @Transactional}.
 * Inside the transaction, the repository count query carries
 * {@code @Lock(PESSIMISTIC_WRITE)}, which Hibernate translates to
 * {@code SELECT COUNT(*) ... FOR UPDATE}. This serialises concurrent opt-out
 * attempts for the same (meal, date) pair, eliminating the TOCTOU race that
 * would otherwise allow the capacity cap to be silently overshot.
 *
 * <p>A {@link DailyOpts} row is written only when a student opts out.
 * An absent row means fully opted-in; no background seeding is performed.
 */
@Service
public class DailyOptsServiceImpl implements DailyOptsService {

    private static final int    CAMPUS_POPULATION   = 1_200;
    private static final double DAILY_MEAL_PLAN_COST = 150.0;

    private final DailyOptsRepository dailyOptsRepository;

    public DailyOptsServiceImpl(DailyOptsRepository dailyOptsRepository) {
        this.dailyOptsRepository = dailyOptsRepository;
    }

    @Override
    @Transactional
    public void optOutFromMeal(String firebaseUid,
                               LocalDate targetDate,
                               MealType mealType,
                               double dynamicCapPercentage) {

        LocalDateTime cutoffDeadline = LocalDateTime.of(targetDate, mealType.getCutoffTime());
        LocalDateTime requestedAt    = LocalDateTime.now();

        // Reject late requests before touching the database; the transaction
        // opens but rolls back immediately with zero lock overhead.
        if (!requestedAt.isBefore(cutoffDeadline)) {
            throw new CutoffDeadlinePassedException(
                mealType.name(), targetDate, cutoffDeadline, requestedAt
            );
        }

        // Acquire FOR UPDATE row locks on all existing opt-out rows for this
        // (meal, date). Any concurrent transaction competing for the same slot
        // blocks here until this transaction commits or rolls back, preventing
        // two transactions from both reading below the cap and both committing.
        long currentOptOutCount = switch (mealType) {
            case BREAKFAST -> dailyOptsRepository.countByOptOutDateAndIsBreakfastOptedOutTrue(targetDate);
            case LUNCH     -> dailyOptsRepository.countByOptOutDateAndIsLunchOptedOutTrue(targetDate);
            case DINNER    -> dailyOptsRepository.countByOptOutDateAndIsDinnerOptedOutTrue(targetDate);
        };

        long absoluteLimit = (long) (CAMPUS_POPULATION * dynamicCapPercentage);

        // >= rather than > so that the cap boundary is never exceeded, even by one.
        // This exception triggers a rollback, atomically releasing all FOR UPDATE locks.
        if (currentOptOutCount >= absoluteLimit) {
            throw new MealCapExceededException(mealType.name(), currentOptOutCount, absoluteLimit);
        }

        DailyOptsId compositeKey = new DailyOptsId(firebaseUid, targetDate);
        DailyOpts optRecord = dailyOptsRepository.findById(compositeKey)
                .orElseGet(() -> new DailyOpts(firebaseUid, targetDate));

        switch (mealType) {
            case BREAKFAST -> optRecord.setBreakfastOptedOut(true);
            case LUNCH     -> optRecord.setLunchOptedOut(true);
            case DINNER    -> optRecord.setDinnerOptedOut(true);
        }

        dailyOptsRepository.save(optRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public BillSummary calculateMonthlyBill(String firebaseUid,
                                            LocalDate startDate,
                                            LocalDate endDate) {

        List<DailyOpts> optOutRecords = dailyOptsRepository
                .findByFirebaseUidAndOptOutDateBetween(firebaseUid, startDate, endDate);

        // +1 to make both bounds inclusive
        long totalDays     = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double grossAmount = totalDays * DAILY_MEAL_PLAN_COST;

        long breakfastOptOuts = optOutRecords.stream().filter(DailyOpts::isBreakfastOptedOut).count();
        long lunchOptOuts     = optOutRecords.stream().filter(DailyOpts::isLunchOptedOut).count();
        long dinnerOptOuts    = optOutRecords.stream().filter(DailyOpts::isDinnerOptedOut).count();

        // Rebate amounts sourced from the enum to keep cost configuration in one place.
        double totalRebate =
                (breakfastOptOuts * MealType.BREAKFAST.getRebateAmount()) +
                (lunchOptOuts     * MealType.LUNCH.getRebateAmount())     +
                (dinnerOptOuts    * MealType.DINNER.getRebateAmount());

        double netPayable = grossAmount - totalRebate;

        return new BillSummary(
                firebaseUid, startDate, endDate,
                totalDays, grossAmount,
                breakfastOptOuts, lunchOptOuts, dinnerOptOuts,
                totalRebate, netPayable
        );
    }
}
