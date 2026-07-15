package com.example.optimeal_api.repository;

import com.example.optimeal_api.entity.DailyOpts;
import com.example.optimeal_api.entity.DailyOptsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DailyOpts. Acts as a pure exception-log store.
 * 
 * Handles point lookups/upserts and historical range scans.
 * Capacity enforcement is delegated to MealDailyCountRepository, 
 * meaning this repository requires no pessimistic locks.
 * 
 * An absent row implies a fully opted-in state.
 */
@Repository
public interface DailyOptsRepository extends JpaRepository<DailyOpts, DailyOptsId> {


    /**
     * Returns the opt-out record. An empty Optional means the student is opted-in.
     *
     * @param id composite key (firebaseUid, optOutDate)
     * @return opt-out record if the student has taken any opt-out action
     */
    Optional<DailyOpts> findById(DailyOptsId id);



    /**
     * Fetches all exception rows for a student within a date range (inclusive).
     * Used for monthly billing computations.
     *
     * @param firebaseUid Firebase UID of the student
     * @param startDate   range lower bound (inclusive)
     * @param endDate     range upper bound (inclusive)
     * @return ordered list of opt-out records within the date window
     */
    @Query("""
            SELECT d FROM DailyOpts d
            WHERE d.id.firebaseUid = :firebaseUid
              AND d.id.optOutDate BETWEEN :startDate AND :endDate
            ORDER BY d.id.optOutDate ASC
            """)
    List<DailyOpts> findByFirebaseUidAndOptOutDateBetween(
            @Param("firebaseUid") String firebaseUid,
            @Param("startDate")   LocalDate startDate,
            @Param("endDate")     LocalDate endDate
    );
}
