package com.example.optimeal_api.controller;

import com.example.optimeal_api.exception.CutoffDeadlinePassedException;
import com.example.optimeal_api.exception.MealCapExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception-to-HTTP mapping for all controllers.
 *
 * <p>All handlers return a uniform JSON schema with {@code timestamp},
 * {@code status}, {@code error}, and {@code message}. Business exceptions
 * include additional structured fields so clients can react without a
 * follow-up request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MealCapExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMealCapExceeded(MealCapExceededException ex) {
        Map<String, Object> body = buildBase(HttpStatus.CONFLICT, ex.getMessage());
        body.put("mealType",      ex.getMealType());
        body.put("currentCount",  ex.getCurrentCount());
        body.put("absoluteLimit", ex.getAbsoluteLimit());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(CutoffDeadlinePassedException.class)
    public ResponseEntity<Map<String, Object>> handleCutoffDeadlinePassed(CutoffDeadlinePassedException ex) {
        Map<String, Object> body = buildBase(HttpStatus.FORBIDDEN, ex.getMessage());
        body.put("mealType",       ex.getMealType());
        body.put("targetDate",     ex.getTargetDate().toString());
        body.put("cutoffDeadline", ex.getCutoffDeadline().toString());
        body.put("requestedAt",    ex.getRequestedAt().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = buildBase(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private Map<String, Object> buildBase(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        return body;
    }
}
