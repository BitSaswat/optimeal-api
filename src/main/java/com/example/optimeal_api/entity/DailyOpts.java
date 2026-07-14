package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * JPA entity for the {@code daily_opts} table.
 *
 * <h3>Exception-Log Semantics</h3>
 * <p>This table is a pure exception log — a row exists <em>only</em> when a
 * student has explicitly opted out of at least one meal on a given date.
 * The absence of a row for {@code (firebaseUid, optOutDate)} is
 * mathematically equivalent to being <em>fully opted-in</em> for that day.
 * No cron job or default-row generation is ever required.
 *
 * <h3>Flag Semantics</h3>
 * <ul>
 *   <li>{@code isBreakfastOptedOut = false} → student is attending breakfast</li>
 *   <li>{@code isBreakfastOptedOut = true}  → student has skipped breakfast</li>
 * </ul>
 * The same logic applies to lunch and dinner flags.
 *
 * <h3>Authentication</h3>
 * <p>Identity is anchored to Firebase UID (opaque String, max 128 chars).
 * There is no relational foreign key to a local {@code users} table —
 * Firebase is the authoritative identity source.
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

    // -------------------------------------------------------------------------
    // Identity — composite PK via @EmbeddedId
    // -------------------------------------------------------------------------

    @EmbeddedId
    private DailyOptsId id;

    // -------------------------------------------------------------------------
    // Opt-out flags
    // Default false: the row only ever exists because at least one flag is true.
    // -------------------------------------------------------------------------

    @Column(name = "is_breakfast_opted_out", nullable = false)
    private boolean isBreakfastOptedOut = false;

    @Column(name = "is_lunch_opted_out", nullable = false)
    private boolean isLunchOptedOut = false;

    @Column(name = "is_dinner_opted_out", nullable = false)
    private boolean isDinnerOptedOut = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected DailyOpts() {
        // Required by JPA
    }

    /**
     * Convenience constructor for service-layer use.
     *
     * @param firebaseUid Firebase UID of the student
     * @param optOutDate  Calendar date for which opt-outs are recorded
     */
    public DailyOpts(String firebaseUid, LocalDate optOutDate) {
        this.id = new DailyOptsId(firebaseUid, optOutDate);
    }

    // -------------------------------------------------------------------------
    // Composite key accessors (delegated to embedded id)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Flag accessors
    // -------------------------------------------------------------------------

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