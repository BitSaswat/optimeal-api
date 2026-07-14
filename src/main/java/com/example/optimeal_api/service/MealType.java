package com.example.optimeal_api.service;

import java.time.LocalTime;

/**
 * Canonical meal slots supported by the OptiMeal system.
 *
 * <p>Each constant encodes its per-day rebate value and the latest time on the
 * target date by which an opt-out request is accepted. Requests arriving at or
 * after the cutoff are rejected with a
 * {@link com.example.optimeal_api.exception.CutoffDeadlinePassedException}.
 *
 * <p>Cutoff policy: BREAKFAST — midnight (00:00) of the target date;
 * LUNCH — 09:00; DINNER — 14:00.
 *
 * <p>Daily cost breakdown (base ₹150): Breakfast ₹40, Lunch ₹65, Dinner ₹45.
 */
public enum MealType {

    BREAKFAST(40.0, LocalTime.MIDNIGHT),
    LUNCH    (65.0, LocalTime.of(9,  0)),
    DINNER   (45.0, LocalTime.of(14, 0));

    /** Amount credited back to the student's bill when this meal is opted out. */
    private final double rebateAmount;

    /** Wall-clock time on the target date after which opt-out requests are rejected. */
    private final LocalTime cutoffTime;

    MealType(double rebateAmount, LocalTime cutoffTime) {
        this.rebateAmount = rebateAmount;
        this.cutoffTime = cutoffTime;
    }

    public double    getRebateAmount() { return rebateAmount; }
    public LocalTime getCutoffTime()   { return cutoffTime;   }
}
