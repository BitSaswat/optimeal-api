package com.example.optimeal_api.repository;

import com.example.optimeal_api.entity.DailyOpts;
import com.example.optimeal_api.entity.DailyOptsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DailyOpts}.
 *
 * <h3>Responsibility</h3>
 * <p>This repository is a pure <em>exception-log</em> store. It handles two
 * operations only:
 * <ol>
 *   <li><b>Point lookup / upsert</b> — read or create the opt-out record for a
 *       {@code (firebaseUid, date)} pair after capacity has already been secured.</li>
 *   <li><b>Historical range scan</b> — fetch all opt-out rows for a student within
 *       a billing window to compute rebates.</li>
 * </ol>
 *
 * <h3>Locking</h3>
 * <p>This repository acquires <em>no</em> pessimistic locks. Capacity enforcement
 * (the vendor opt-out cap) is handled entirely by the atomic
 * {@code INSERT ... ON CONFLICT DO UPDATE} in
 * {@link com.example.optimeal_api.repository.MealDailyCountRepository#incrementIfBelowLimit},
 * which operates on a single aggregate row in {@code meal_daily_counts} rather than
 * scanning and locking N rows in this table.
 *
 * <h3>Exception-log contract</h3>
 * <p>A row exists in {@code daily_opts} <em>only</em> when a student has
 * explicitly opted out of at least one meal on a given date. Callers must treat
 * an empty {@link Optional} from {@link #findById(DailyOptsId)} as a
 * fully opted-in state — no default rows are ever seeded.
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
