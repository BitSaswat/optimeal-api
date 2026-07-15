package com.example.optimeal_api.repository;

import com.example.optimeal_api.entity.MealDailyCount;
import com.example.optimeal_api.entity.MealDailyCountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repository for the {@code meal_daily_counts} aggregate table.
 *
 * <h3>Atomic Upsert Strategy</h3>
 * <p>The core method {@link #incrementIfBelowLimit} issues a single native
 * PostgreSQL {@code INSERT ... ON CONFLICT DO UPDATE} statement. This collapses
 * the old read-check-write cycle (which required locking every opted-out row in
 * {@code daily_opts}) into a single O(1) row-level operation:
 *
 * <ul>
 *   <li>If no counter row exists for {@code (date, mealType)}, one is inserted
 *       with {@code opt_out_count = 1} — provided {@code 1 <= :limit}.</li>
 *   <li>If a row exists and {@code opt_out_count < :limit}, the count is
 *       atomically incremented by 1.</li>
 *   <li>If the limit is already reached, the {@code WHERE} predicate on the
 *       {@code UPDATE} clause prevents modification, returning 0 rows affected.</li>
 * </ul>
 *
 * <p>The caller must treat a return value of {@code 0} as a capacity-exceeded
 * signal and throw a
 * {@link com.example.optimeal_api.exception.MealCapExceededException}.
 *
 * <p><b>Concurrency:</b> PostgreSQL serialises concurrent upserts on the same
 * {@code (opt_out_date, meal_type)} primary-key row, so no separate
 * application-level lock is required. Two threads racing to increment the last
 * available slot will see one succeed (returns 1) and one fail (returns 0).
 */
@Repository
public interface MealDailyCountRepository extends JpaRepository<MealDailyCount, MealDailyCountId> {

    /**
     * Atomically increments the opt-out counter for the given meal and date,
     * but only if the current count is strictly below {@code absoluteLimit}.
     *
     * <p>Uses {@code INSERT ... ON CONFLICT DO UPDATE} so the operation is safe
     * to call regardless of whether a counter row already exists.
     *
     * @param optOutDate    the date of the meal
     * @param mealType      the meal type string ({@code "BREAKFAST"}, {@code "LUNCH"}, {@code "DINNER"})
     * @param absoluteLimit the maximum number of opt-outs allowed; increment is
     *                      skipped when the current count reaches this value
     * @return number of rows modified: {@code 1} on success, {@code 0} if the
     *         cap has been reached
     */
    @Modifying
    @Query(value = """
            INSERT INTO meal_daily_counts (opt_out_date, meal_type, opt_out_count)
            VALUES (:optOutDate, :mealType, 1)
            ON CONFLICT (opt_out_date, meal_type) DO UPDATE
                SET opt_out_count = meal_daily_counts.opt_out_count + 1
                WHERE meal_daily_counts.opt_out_count < :absoluteLimit
            """, nativeQuery = true)
    int incrementIfBelowLimit(
            @Param("optOutDate")    LocalDate optOutDate,
            @Param("mealType")      String    mealType,
            @Param("absoluteLimit") long      absoluteLimit
    );
}
