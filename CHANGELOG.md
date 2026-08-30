# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-08-30

### Added
- **Financial Precision**: Switched all discount representations from `double` to `BigDecimal` (stored as `NUMERIC(10, 2)` in PostgreSQL).
- **Kafka Partition Ordering**: Added `productId` as the message key on Kafka producer to enforce per-product ordered event consumption across consumers.
- **Idempotent Kafka Producer**: Configured `enable.idempotence: true`, `acks: all`, `retries: MAX_VALUE`, and bounded in-flight requests.
- **Atomic Batch Upsert**: Introduced PostgreSQL `ON CONFLICT (product_id) DO UPDATE` batch execution to prevent race conditions and eliminate N+1 queries.
- **Validation & RFC 7807 Error Handling**: Added JSR-380 annotations (`@NotNull`, `@Positive`, `@DecimalMin`, `@DecimalMax`) and `GlobalExceptionHandler` returning `ProblemDetail` payloads.
- **RESTful Endpoints**: Added `GET /api/v1/discounts/paged` (pagination metadata), `PUT /api/v1/discounts/{productId}`, and `DELETE /api/v1/discounts/{productId}` (returning 204/404).
- **Architecture & Quality**: Standardized package naming to `com.serjnn.discountservice`, added `DiscountMapper` (MapStruct), audit columns (`created_at`, `updated_at`), database check constraints (`CHECK (discount >= 0.00 AND discount <= 100.00)`), and HikariCP connection pool tuning.
- **Observability**: Added Micrometer Prometheus registry and Actuator Kubernetes probes (`/actuator/health/liveness`, `/actuator/health/readiness`).
- **Interactive Documentation**: Integrated SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`).
- **Containerization & CI/CD**: Added multi-stage non-root `Dockerfile`, `docker-compose.yml` with Prometheus/Zipkin/Kafka/PostgreSQL, GitHub Actions CI workflow, `.editorconfig`, and Kubernetes manifests in `k8s/`.
