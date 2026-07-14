package com.example.optimeal_api.exception;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thrown when a student attempts to modify an opt-out state after the per-meal
 * cutoff deadline, or for a date that has already elapsed.
 *
 * <p>Cutoff validation is performed before any database interaction, so this
 * exception carries zero locking overhead.
 */
public class CutoffDeadlinePassedException extends RuntimeException {

    private final String mealType;
    private final LocalDate targetDate;
    private final LocalDateTime cutoffDeadline;
    private final LocalDateTime requestedAt;

    public CutoffDeadlinePassedException(
            String mealType,
            LocalDate targetDate,
            LocalDateTime cutoffDeadline,
            LocalDateTime requestedAt) {

        super(String.format(
            "Opt-out window closed for meal [%s] on [%s]. Deadline was [%s], request arrived at [%s].",
            mealType, targetDate, cutoffDeadline, requestedAt
        ));
        this.mealType = mealType;
        this.targetDate = targetDate;
        this.cutoffDeadline = cutoffDeadline;
        this.requestedAt = requestedAt;
    }

    public String        getMealType()       { return mealType;       }
    public LocalDate     getTargetDate()     { return targetDate;     }
    public LocalDateTime getCutoffDeadline() { return cutoffDeadline; }
    public LocalDateTime getRequestedAt()    { return requestedAt;    }
}
