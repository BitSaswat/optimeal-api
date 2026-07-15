# OptiMeal API: Campus Cafeteria Anti-Waste & Reconciliation Engine

An enterprise-grade Spring Boot API designed to dynamically manage meal-specific opt-outs, forecast kitchen inventory, and automate financial reconciliation across a multi-hostel campus network.

## Core Architecture
* **Rolling Temporal Validation:** Enforces strict meal-specific cutoff deadlines (e.g., midnight for breakfast) using native `java.time` to allow accurate vendor preparation.
* **O(1) Atomic Capacity Enforcement:** Replaces traditional pessimistic locking with an atomic PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` counter, preventing race conditions during high-traffic cutoff windows without N-row locking overhead.
* **ACID Financial Reconciliation:** Automated background engine using Spring `@Scheduled` and `@Transactional` to process monthly student mess bill rebates without duplicate crediting risk.
* **Role-Based Access Control (RBAC):** Securely isolates API data views, providing students with financial tracking while serving anonymized, real-time aggregate counts to kitchen staff.

## Tech Stack
* **Backend:** Java, Spring Boot, Spring Security, Spring Data JPA
* **Database:** PostgreSQL
* **Architecture:** RESTful API, Multi-Tenant Design

## Database Schema
* **daily_opts (Exception Log):** A boolean-based, highly flattened schema for lightning-fast $O(1)$ row-level reads. A row is only created when a student explicitly opts out, significantly reducing database footprint.
* **meal_daily_counts (Aggregate Counter):** A targeted aggregate table for atomic $O(1)$ capacity enforcement per meal type and date, avoiding heavy `JOIN` and `SELECT ... FOR UPDATE` operations.