package com.example.optimeal_api.exception;

/**
 * Thrown when the number of students opted out of a specific meal on a given date
 * meets or exceeds the dynamically computed allocation cap.
 *
 * <p>This exception is raised inside a {@code PESSIMISTIC_WRITE}-locked transaction.
 * The unchecked propagation triggers a rollback, releasing all {@code FOR UPDATE}
 * row locks atomically and preventing any concurrent transaction from committing
 * a write that would overshoot the threshold.
 */
public class MealCapExceededException extends RuntimeException {

    private final String mealType;
    private final long currentCount;
    private final long absoluteLimit;

    public MealCapExceededException(String mealType, long currentCount, long absoluteLimit) {
        super(String.format(
            "Opt-out cap reached for meal [%s]: %d/%d slots consumed. No further opt-outs are permitted.",
            mealType, currentCount, absoluteLimit
        ));
        this.mealType = mealType;
        this.currentCount = currentCount;
        this.absoluteLimit = absoluteLimit;
    }

    public String getMealType()      { return mealType;      }
    public long   getCurrentCount()  { return currentCount;  }
    public long   getAbsoluteLimit() { return absoluteLimit; }
}
