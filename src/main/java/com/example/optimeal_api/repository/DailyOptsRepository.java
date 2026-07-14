package com.example.optimeal_api.repository;

import com.example.optimeal_api.entity.DailyOpts;
import com.example.optimeal_api.entity.DailyOptsId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DailyOpts}.
 *
 * <p><b>Locking strategy:</b> The three meal-count methods acquire a
 * {@code SELECT ... FOR UPDATE} pessimistic write lock on all matching rows.
 * This serialises concurrent opt-out/check-in requests against the same date
 * so that no two transactions can simultaneously read the pre-cap count,
 * both decide "capacity available", and both commit — causing a silent
 * overshoot of the per-meal profit threshold.
 *
 * <p><b>Exception-log contract:</b> Only rows representing an active opt-out
 * are present in this table. Callers must treat an empty {@link Optional}
 * from {@link #findById(DailyOptsId)} as a fully opted-in state.
 */
@Repository
public interface DailyOptsRepository extends JpaRepository<DailyOpts, DailyOptsId> {

    // -------------------------------------------------------------------------
    // Non-locking point lookup — exception-log read path
    // -------------------------------------------------------------------------

    /**
     * Returns the opt-out record for a specific student on a specific date.
     *
     * <p>An empty {@link Optional} means the student is fully opted-in for
     * that date; no lock is acquired on this read path.
     *
     * @param id composite key {@code (firebaseUid, optOutDate)}
     * @return opt-out record if the student has taken any opt-out action
     */
    Optional<DailyOpts> findById(DailyOptsId id);

    // -------------------------------------------------------------------------
    // Pessimistic-write count queries — capacity enforcement
    // -------------------------------------------------------------------------

    /**
     * Counts students who have opted out of breakfast on {@code optOutDate}.
     *
     * <p>Acquires a {@code SELECT ... FOR UPDATE} lock on all matching rows,
     * blocking any concurrent transaction from modifying those rows until the
     * current transaction commits or rolls back. Use inside a
     * {@code @Transactional} method that enforces the meal-capacity cap.
     *
     * @param optOutDate the date to evaluate
     * @return number of students opted out of breakfast
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT COUNT(d) FROM DailyOpts d WHERE d.id.optOutDate = :optOutDate AND d.isBreakfastOptedOut = TRUE")
    long countByOptOutDateAndIsBreakfastOptedOutTrue(@Param("optOutDate") LocalDate optOutDate);

    /**
     * Counts students who have opted out of lunch on {@code optOutDate}.
     *
     * <p>Acquires a {@code SELECT ... FOR UPDATE} lock; see
     * {@link #countByOptOutDateAndIsBreakfastOptedOutTrue} for lock semantics.
     *
     * @param optOutDate the date to evaluate
     * @return number of students opted out of lunch
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT COUNT(d) FROM DailyOpts d WHERE d.id.optOutDate = :optOutDate AND d.isLunchOptedOut = TRUE")
    long countByOptOutDateAndIsLunchOptedOutTrue(@Param("optOutDate") LocalDate optOutDate);

    /**
     * Counts students who have opted out of dinner on {@code optOutDate}.
     *
     * <p>Acquires a {@code SELECT ... FOR UPDATE} lock; see
     * {@link #countByOptOutDateAndIsBreakfastOptedOutTrue} for lock semantics.
     *
     * @param optOutDate the date to evaluate
     * @return number of students opted out of dinner
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT COUNT(d) FROM DailyOpts d WHERE d.id.optOutDate = :optOutDate AND d.isDinnerOptedOut = TRUE")
    long countByOptOutDateAndIsDinnerOptedOutTrue(@Param("optOutDate") LocalDate optOutDate);

    // -------------------------------------------------------------------------
    // Historical range query — monthly billing summation
    // -------------------------------------------------------------------------

    /**
     * Fetches all exception rows for a student where the opt-out date falls
     * within [{@code startDate}, {@code endDate}], inclusive on both bounds.
     *
     * <p>Intended for real-time monthly billing computation: iterate the
     * returned list and tally opted-out meal flags to derive the deduction
     * amount from the student's meal plan subscription.
     *
     * @param firebaseUid Firebase UID of the student
     * @param startDate   range lower bound (inclusive)
     * @param endDate     range upper bound (inclusive)
     * @return ordered list of opt-out records within the date window
     */
    @Query("""
            SELECT d FROM DailyOpts d
            WHERE d.id.firebaseUid = :firebaseUid
              AND d.id.optOutDate BETWEEN :startDate AND :endDate
            ORDER BY d.id.optOutDate ASC
            """)
    List<DailyOpts> findByFirebaseUidAndOptOutDateBetween(
            @Param("firebaseUid") String firebaseUid,
            @Param("startDate")   LocalDate startDate,
            @Param("endDate")     LocalDate endDate
    );
}
