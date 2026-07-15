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
 * Repository for the meal_daily_counts aggregate table.
 * 
 * Uses an atomic INSERT ... ON CONFLICT DO UPDATE strategy via incrementIfBelowLimit
 * to enforce the vendor cap in a single O(1) row-level operation.
 * 
 * A return value of 0 indicates the capacity limit has been reached.
 * PostgreSQL handles the concurrent serialisation on the primary key, avoiding
 * application-level locks.
 */
@Repository
public interface MealDailyCountRepository extends JpaRepository<MealDailyCount, MealDailyCountId> {

    /**
     * Atomically increments the opt-out counter if below the absolute limit.
     * Uses UPSERT to handle both initial insertion and subsequent increments safely.
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
