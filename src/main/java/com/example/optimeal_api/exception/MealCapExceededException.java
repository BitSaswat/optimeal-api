package com.example.optimeal_api.exception;

/**
 * Thrown when the number of students opted out of a specific meal on a given date
 * meets or exceeds the dynamically computed allocation cap.
 *
 * <p>This exception is raised by {@link com.example.optimeal_api.service.DailyOptsServiceImpl}
 * when the atomic {@code INSERT ... ON CONFLICT DO UPDATE} in
 * {@link com.example.optimeal_api.repository.MealDailyCountRepository#incrementIfBelowLimit}
 * returns 0 rows affected — meaning PostgreSQL's conditional update clause determined
 * that {@code opt_out_count >= absoluteLimit} and refused to increment.
 * No pessimistic row locks are held when this exception is thrown.
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
