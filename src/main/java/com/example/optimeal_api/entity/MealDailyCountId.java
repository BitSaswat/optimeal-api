package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for {@link MealDailyCount}.
 *
 * <p>Maps to columns {@code (opt_out_date, meal_type)}.
 * Must implement {@link Serializable} and provide value-based
 * {@code equals}/{@code hashCode} as mandated by the JPA spec §2.4.
 */
@Embeddable
public class MealDailyCountId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "opt_out_date", nullable = false)
    private LocalDate optOutDate;

    @Column(name = "meal_type", nullable = false, length = 10)
    private String mealType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected MealDailyCountId() {
        // Required by JPA
    }

    public MealDailyCountId(LocalDate optOutDate, String mealType) {
        this.optOutDate = optOutDate;
        this.mealType   = mealType;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public LocalDate getOptOutDate() { return optOutDate; }
    public String    getMealType()   { return mealType;   }

    // -------------------------------------------------------------------------
    // Identity — value-based equality required by JPA spec §2.4
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MealDailyCountId that)) return false;
        return Objects.equals(optOutDate, that.optOutDate)
            && Objects.equals(mealType,   that.mealType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optOutDate, mealType);
    }
}
