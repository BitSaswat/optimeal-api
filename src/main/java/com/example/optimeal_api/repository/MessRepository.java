package com.example.optimeal_api.repository;

import com.example.optimeal_api.entity.DailyOpts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessRepository extends JpaRepository<DailyOpts, Long> {
    // Spring Data JPA handles all CRUD operations automatically here.
}