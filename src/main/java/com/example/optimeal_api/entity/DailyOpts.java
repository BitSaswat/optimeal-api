package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * JPA entity mapping for the daily_opts table.
 *
 * This table acts as an exception log. A row only exists if a student opts out
 * of a meal. If no row exists for a given date, the student is fully opted-in.
 * 
 * Identity is tied to the Firebase UID rather than a local users table.
 */
@Entity
@Table(
    name = "daily_opts",
    uniqueConstraints = @UniqueConstraint(
        name = "uidx_daily_opts_uid_date",
        columnNames = { "firebase_uid", "opt_out_date" }
    )
)
public class DailyOpts {

    @EmbeddedId
    private DailyOptsId id;

    // Default false: row existence implies at least one opt-out.
    @Column(name = "is_breakfast_opted_out", nullable = false)
    private boolean isBreakfastOptedOut = false;

    @Column(name = "is_lunch_opted_out", nullable = false)
    private boolean isLunchOptedOut = false;

    @Column(name = "is_dinner_opted_out", nullable = false)
    private boolean isDinnerOptedOut = false;


    protected DailyOpts() {}

    /**
     * Convenience constructor for service-layer use.
     *
     * @param firebaseUid Firebase UID of the student
     * @param optOutDate  Calendar date for which opt-outs are recorded
     */
    public DailyOpts(String firebaseUid, LocalDate optOutDate) {
        this.id = new DailyOptsId(firebaseUid, optOutDate);
    }


    public DailyOptsId getId() {
        return id;
    }

    public void setId(DailyOptsId id) {
        this.id = id;
    }

    /** Convenience accessor — delegates to embedded id. */
    public String getFirebaseUid() {
        return id != null ? id.getFirebaseUid() : null;
    }

    /** Convenience accessor — delegates to embedded id. */
    public LocalDate getOptOutDate() {
        return id != null ? id.getOptOutDate() : null;
    }


    public boolean isBreakfastOptedOut() {
        return isBreakfastOptedOut;
    }

    public void setBreakfastOptedOut(boolean breakfastOptedOut) {
        this.isBreakfastOptedOut = breakfastOptedOut;
    }

    public boolean isLunchOptedOut() {
        return isLunchOptedOut;
    }

    public void setLunchOptedOut(boolean lunchOptedOut) {
        this.isLunchOptedOut = lunchOptedOut;
    }

    public boolean isDinnerOptedOut() {
        return isDinnerOptedOut;
    }

    public void setDinnerOptedOut(boolean dinnerOptedOut) {
        this.isDinnerOptedOut = dinnerOptedOut;
    }
}