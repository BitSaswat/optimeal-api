package com.example.optimeal_api.service;

import java.time.LocalDate;

/**
 * Immutable billing summary returned by {@link DailyOptsService#calculateMonthlyBill}.
 *
 * @param firebaseUid      Student identity
 * @param startDate        Billing window lower bound (inclusive)
 * @param endDate          Billing window upper bound (inclusive)
 * @param totalDays        Calendar days in the window
 * @param grossAmount      Full meal plan cost before rebates (totalDays × ₹150)
 * @param breakfastOptOuts Number of breakfast opt-outs in the window
 * @param lunchOptOuts     Number of lunch opt-outs in the window
 * @param dinnerOptOuts    Number of dinner opt-outs in the window
 * @param totalRebate      Sum of all meal rebates deducted
 * @param netPayable       Gross amount minus total rebate
 */
public record BillSummary(
        String    firebaseUid,
        LocalDate startDate,
        LocalDate endDate,
        long      totalDays,
        double    grossAmount,
        long      breakfastOptOuts,
        long      lunchOptOuts,
        long      dinnerOptOuts,
        double    totalRebate,
        double    netPayable
) {}
