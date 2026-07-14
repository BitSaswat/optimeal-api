-- =============================================================================
-- Table  : daily_opts
-- Purpose: Exception-log for meal opt-outs.
--          A row is inserted ONLY when a student opts out of ≥1 meal slot.
--          Absence of a row for (firebase_uid, opt_out_date) => fully opted-in.
-- =============================================================================

CREATE TABLE IF NOT EXISTS daily_opts (
    firebase_uid            VARCHAR(128)    NOT NULL,
    opt_out_date            DATE            NOT NULL,
    is_breakfast_opted_out  BOOLEAN         NOT NULL DEFAULT FALSE,
    is_lunch_opted_out      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_dinner_opted_out     BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_daily_opts PRIMARY KEY (firebase_uid, opt_out_date)
);

-- Explicit composite index to accelerate point lookups and range scans
-- on (firebase_uid, opt_out_date) under concurrent read/write load.
-- The PRIMARY KEY constraint already enforces uniqueness; this named index
-- is declared separately so it can be monitored, rebuilt, or dropped
-- independently without touching the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS uidx_daily_opts_uid_date
    ON daily_opts (firebase_uid, opt_out_date);

-- Partial index: covers "fetch all active opt-outs for a student" queries.
-- Only rows where at least one meal is opted out are indexed, keeping the
-- index compact and preventing bloat from accidental all-FALSE inserts.
CREATE INDEX IF NOT EXISTS idx_daily_opts_uid_active
    ON daily_opts (firebase_uid)
    WHERE is_breakfast_opted_out = TRUE
       OR is_lunch_opted_out     = TRUE
       OR is_dinner_opted_out    = TRUE;
