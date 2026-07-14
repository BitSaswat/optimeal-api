package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for {@link DailyOpts}.
 *
 * <p>Maps to columns (firebase_uid, opt_out_date).
 * Must implement {@link Serializable} and provide value-based
 * {@code equals}/{@code hashCode} as mandated by the JPA specification
 * for embeddable identity classes.
 */
@Embeddable
public class DailyOptsId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "opt_out_date", nullable = false)
    private LocalDate optOutDate;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected DailyOptsId() {
        // Required by JPA
    }

    public DailyOptsId(String firebaseUid, LocalDate optOutDate) {
        this.firebaseUid = firebaseUid;
        this.optOutDate  = optOutDate;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public LocalDate getOptOutDate() {
        return optOutDate;
    }

    public void setOptOutDate(LocalDate optOutDate) {
        this.optOutDate = optOutDate;
    }

    // -------------------------------------------------------------------------
    // Identity — value-based equality required by JPA spec §2.4
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyOptsId that)) return false;
        return Objects.equals(firebaseUid, that.firebaseUid)
            && Objects.equals(optOutDate,  that.optOutDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firebaseUid, optOutDate);
    }
}
