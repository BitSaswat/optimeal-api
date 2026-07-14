package com.example.optimeal_api.controller;

import com.example.optimeal_api.service.BillSummary;
import com.example.optimeal_api.service.DailyOptsService;
import com.example.optimeal_api.service.MealType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for student meal opt-out and billing operations.
 *
 * <p>The caller's identity is sourced exclusively from the {@code "firebaseUid"}
 * request attribute set by {@link com.example.optimeal_api.security.FirebaseTokenFilter}.
 * It is never read from the request body or query string to prevent identity spoofing.
 */
@RestController
@RequestMapping("/api/v1/meals")
public class DailyOptsController {

    private final DailyOptsService dailyOptsService;

    public DailyOptsController(DailyOptsService dailyOptsService) {
        this.dailyOptsService = dailyOptsService;
    }

    @PostMapping("/opt-out")
    public ResponseEntity<Void> optOut(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestBody OptOutRequest requestBody) {

        // mealType is accepted as a String at the HTTP boundary so that an
        // unrecognised value produces a structured 400 via GlobalExceptionHandler
        // rather than an uncontrolled Jackson deserialization error.
        MealType mealType;
        try {
            mealType = MealType.valueOf(requestBody.mealType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unknown mealType '" + requestBody.mealType() + "'. Accepted values: BREAKFAST, LUNCH, DINNER."
            );
        }

        dailyOptsService.optOutFromMeal(
                firebaseUid,
                requestBody.targetDate(),
                mealType,
                requestBody.dynamicCapPercentage()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/billing")
    public ResponseEntity<BillSummary> getBilling(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        BillSummary summary = dailyOptsService.calculateMonthlyBill(firebaseUid, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Deserialization target for the opt-out POST body.
     *
     * <p>{@code mealType} is a {@code String} rather than a {@link MealType} enum
     * so that invalid values are caught and mapped to a structured 400 response.
     */
    public record OptOutRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            String mealType,
            double dynamicCapPercentage
    ) {}
}
