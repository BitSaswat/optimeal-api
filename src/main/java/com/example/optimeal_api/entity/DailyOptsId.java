package com.example.optimeal_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for DailyOpts.
 * Maps to (firebase_uid, opt_out_date).
 */
@Embeddable
public class DailyOptsId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "opt_out_date", nullable = false)
    private LocalDate optOutDate;


    protected DailyOptsId() {}

    public DailyOptsId(String firebaseUid, LocalDate optOutDate) {
        this.firebaseUid = firebaseUid;
        this.optOutDate  = optOutDate;
    }


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
