-- =============================================================================
-- Table  : meal_daily_counts
-- Purpose: Aggregate opt-out counter per (date, meal_type) to enforce vendor caps.
-- =============================================================================

CREATE TABLE IF NOT EXISTS meal_daily_counts (
    opt_out_date  DATE         NOT NULL,
    meal_type     VARCHAR(10)  NOT NULL,
    opt_out_count INTEGER      NOT NULL DEFAULT 0
        CONSTRAINT chk_meal_daily_counts_non_negative CHECK (opt_out_count >= 0),

    CONSTRAINT pk_meal_daily_counts PRIMARY KEY (opt_out_date, meal_type),
    CONSTRAINT chk_meal_daily_counts_meal_type
        CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER'))
);

-- Supports forward range scans (e.g. "all meals for the next 7 days") that
-- an admin/vendor dashboard might issue when planning weekly inventory.
CREATE INDEX IF NOT EXISTS idx_meal_daily_counts_date
    ON meal_daily_counts (opt_out_date);
