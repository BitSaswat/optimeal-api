package com.example.optimeal_api.service;

import com.example.optimeal_api.entity.DailyOpts;
import com.example.optimeal_api.entity.DailyOptsId;
import com.example.optimeal_api.entity.MealDailyCountId;
import com.example.optimeal_api.exception.CutoffDeadlinePassedException;
import com.example.optimeal_api.exception.MealCapExceededException;
import com.example.optimeal_api.repository.DailyOptsRepository;
import com.example.optimeal_api.repository.MealDailyCountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation of {@link DailyOptsService}.
 *
 * <h3>Opt-out flow</h3>
 * <ol>
 *   <li><b>Temporal gate:</b> the cutoff deadline for the requested meal is
 *       evaluated using {@code java.time} <em>before</em> any database interaction.
 *       Late requests are rejected immediately with zero lock overhead.</li>
 *   <li><b>Atomic capacity enforcement:</b> a single PostgreSQL
 *       {@code INSERT ... ON CONFLICT DO UPDATE} issued by
 *       {@link com.example.optimeal_api.repository.MealDailyCountRepository#incrementIfBelowLimit}
 *       atomically increments the per-meal aggregate counter in
 *       {@code meal_daily_counts} <em>only if</em> the current count is below the
 *       computed cap. This is an O(1) single-row operation — no full-table scan or
 *       pessimistic lock on {@code daily_opts} rows is required.</li>
 *   <li><b>Exception-log write:</b> only after a slot is secured does the service
 *       upsert the boolean flag in {@code daily_opts}. An absent row implies the
 *       student is fully opted-in; no background seeding is ever performed.</li>
 * </ol>
 *
 * <h3>Billing flow</h3>
 * <p>{@link #calculateMonthlyBill} is a pure read: it fetches opt-out exception
 * rows for the given date window and derives the financial breakdown
 * arithmetically from the flags stored in {@code daily_opts}. No database
 * state is modified.
 */
@Service
public class DailyOptsServiceImpl implements DailyOptsService {

    /**
     * Total number of students on campus. Sourced from {@code application.properties}
     * so that capacity can be reconfigured without a recompile.
     */
    @Value("${campus.population}")
    private int campusPopulation;

    /**
     * Base daily meal-plan cost in rupees. Sourced from {@code application.properties}
     * so that pricing updates do not require a code change.
     */
    @Value("${campus.daily-meal-plan-cost}")
    private double dailyMealPlanCost;

    private final DailyOptsRepository     dailyOptsRepository;
    private final MealDailyCountRepository mealDailyCountRepository;

    public DailyOptsServiceImpl(DailyOptsRepository     dailyOptsRepository,
                                MealDailyCountRepository mealDailyCountRepository) {
        this.dailyOptsRepository     = dailyOptsRepository;
        this.mealDailyCountRepository = mealDailyCountRepository;
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

        long absoluteLimit = (long) (campusPopulation * dynamicCapPercentage);

        // Atomically increment the aggregate counter for this (date, meal_type) pair
        // using a single INSERT ... ON CONFLICT DO UPDATE statement.
        //
        // The UPDATE clause fires only when opt_out_count < absoluteLimit, so
        // PostgreSQL enforces the cap within a single SQL round-trip on a single
        // O(1) row — replacing the previous O(N) SELECT COUNT(*) FOR UPDATE that
        // locked every opted-out row in daily_opts.
        //
        // Return value: 1 = counter incremented (slot secured),
        //               0 = cap already reached (reject the request).
        int rowsAffected = mealDailyCountRepository.incrementIfBelowLimit(
                targetDate, mealType.name(), absoluteLimit
        );

        if (rowsAffected == 0) {
            // Cap is full — read the current count for the error payload.
            long currentCount = mealDailyCountRepository
                    .findById(new MealDailyCountId(targetDate, mealType.name()))
                    .map(c -> (long) c.getOptOutCount())
                    .orElse(absoluteLimit);
            throw new MealCapExceededException(mealType.name(), currentCount, absoluteLimit);
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
        double grossAmount = totalDays * dailyMealPlanCost;

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
