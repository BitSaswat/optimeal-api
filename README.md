# OptiMeal API: Campus Cafeteria Anti-Waste & Reconciliation Engine

An enterprise-grade Spring Boot API designed to dynamically manage meal-specific opt-outs, forecast kitchen inventory, and automate financial reconciliation across a multi-hostel campus network.

## Core Architecture
* **Rolling Temporal Validation:** Enforces strict meal-specific cutoff deadlines (e.g., midnight for breakfast) using native `java.time` to allow accurate vendor preparation.
* **Concurrency Control:** Utilizes PostgreSQL Pessimistic Locking (`FOR UPDATE`) to enforce daily vendor capacity limits, preventing race conditions during high-traffic cutoff windows.
* **ACID Financial Reconciliation:** Automated background engine using Spring `@Scheduled` and `@Transactional` to process monthly student mess bill rebates without duplicate crediting risk.
* **Role-Based Access Control (RBAC):** Securely isolates API data views, providing students with financial tracking while serving anonymized, real-time aggregate counts to kitchen staff.

## Tech Stack
* **Backend:** Java, Spring Boot, Spring Security, Spring Data JPA
* **Database:** PostgreSQL
* **Architecture:** RESTful API, Multi-Tenant Design

## Database Schema (Flat Optimization)
To ensure lightning-fast aggregations for vendor dashboards, the database relies on a highly flattened, boolean-based schema (`daily_opts`) rather than complex enum state machines. This allows $O(1)$ row-level reads for daily meal tracking and prevents heavy `JOIN` overhead during high-traffic API calls.