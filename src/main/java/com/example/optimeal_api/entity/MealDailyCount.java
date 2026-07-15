package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * JPA entity for the {@code meal_daily_counts} aggregate table.
 *
 * <h3>Purpose</h3>
 * <p>Stores a running total of opt-outs per {@code (opt_out_date, meal_type)}
 * pair. A single row replaces an O(N) {@code SELECT COUNT(*) FOR UPDATE}
 * scan across all opted-out rows in {@code daily_opts}, reducing the locking
 * surface to a single row per meal per day.
 *
 * <h3>Write Contract</h3>
 * <p>Rows are created lazily via an atomic
 * {@code INSERT ... ON CONFLICT DO UPDATE SET opt_out_count = opt_out_count + 1}
 * issued by {@link com.example.optimeal_api.repository.MealDailyCountRepository}.
 * The update only fires when {@code opt_out_count < :limit}, so the cap is
 * enforced atomically without a separate read-check-write cycle.
 */
@Entity
@Table(name = "meal_daily_counts")
public class MealDailyCount {

    @EmbeddedId
    private MealDailyCountId id;

    @Column(name = "opt_out_count", nullable = false)
    private int optOutCount = 0;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected MealDailyCount() {
        // Required by JPA
    }

    public MealDailyCount(MealDailyCountId id) {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public MealDailyCountId getId()         { return id;           }
    public int               getOptOutCount() { return optOutCount; }
}
