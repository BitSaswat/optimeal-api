package com.example.optimeal_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_opts")
public class DailyOpts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false)
    private boolean check_b = true;

    @Column(nullable = false)
    private boolean check_l = true;

    @Column(nullable = false)
    private boolean check_d = true;

    // Getters and Setters omitted for brevity
}