package com.example.optimeal_api.service;

import java.time.LocalDate;

/**
 * Business interface for daily meal opt-out management and billing computation.
 *
 * <p>Mutating operations are transactional and enforce pessimistic locking.
 * Read operations are computation-driven and acquire no locks.
 */
public interface DailyOptsService {

    /**
     * Records a student's opt-out from a specific meal on a given date.
     *
     * <p>Enforces the per-meal temporal cutoff, acquires a pessimistic write lock
     * via the repository count query to guard against concurrent cap overshoot,
     * and persists the opt-out flag following the exception-log pattern.
     *
     * @param firebaseUid          Firebase UID of the requesting student
     * @param targetDate           Date for which the opt-out applies
     * @param mealType             Target meal slot
     * @param dynamicCapPercentage Fraction of campus population allowed to opt out (e.g. {@code 0.30})
     * @throws com.example.optimeal_api.exception.CutoffDeadlinePassedException
     *         if the request arrives after the meal's cutoff time
     * @throws com.example.optimeal_api.exception.MealCapExceededException
     *         if the opt-out count for that meal and date has reached the cap
     */
    void optOutFromMeal(String firebaseUid,
                        LocalDate targetDate,
                        MealType mealType,
                        double dynamicCapPercentage);

    /**
     * Computes the net payable meal-plan bill for a student over a date range.
     *
     * <p>Read-only: fetches opt-out exception rows and derives the financial
     * breakdown arithmetically. No database state is modified.
     *
     * @param firebaseUid Firebase UID of the student
     * @param startDate   Billing window lower bound (inclusive)
     * @param endDate     Billing window upper bound (inclusive)
     * @return a fully populated {@link BillSummary}
     */
    BillSummary calculateMonthlyBill(String firebaseUid,
                                     LocalDate startDate,
                                     LocalDate endDate);
}
