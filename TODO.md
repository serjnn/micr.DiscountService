# Discount Service: Roadmap to Perfection (TODO)

This document provides a comprehensive, actionable roadmap detailing every gap, bug, architectural flaw, testing deficiency, and operational requirement separating the current **Discount Service** from a production-grade, enterprise-ready, and resilient microservice.

---

## 📊 Maturity Assessment Overview

| Domain | Current Status | Target State | Priority |
|---|---|---|---|
| **Correctness & REST Semantics** | ⚠️ Bugs in HTTP status codes, missing 404, no validation | Strict RFC compliance, Bean Validation, RFC 7807 Problem Details | **P0 / P1** |
| **Distributed Messaging & Consistency** | ⚠️ Dual-write vulnerability, non-keyed Kafka events | Transactional Outbox pattern, keyed partitioning, idempotent producer | **P0 / P1** |
| **Database & Concurrency** | ⚠️ Check-then-act race conditions, N+1 queries, `double` float types | Atomic upserts, batching, `BigDecimal` / `NUMERIC`, audit fields | **P0 / P1** |
| **Testing & Quality Assurance** | ⚠️ Zero unit tests, only happy-path integration tests | 85%+ coverage, WebMvc slice tests, Mockito unit tests, ArchUnit, Pitest | **P1** |
| **Observability & Monitoring** | ⚠️ Minimal tracing, unexposed metrics, unformatted logs | Prometheus metrics, OpenTelemetry, Logback JSON structured logs, Health probes | **P1 / P2** |
| **Security & Resilience** | ❌ No authentication, no rate limiting, no circuit breaker | Spring Security / OAuth2, Bucket4j rate limiting, Resilience4j | **P1 / P2** |
| **CI/CD & DevOps** | ⚠️ Root container execution, no docker-compose, no CI workflow | Multi-stage non-root Docker, docker-compose, GitHub Actions, K8s manifests | **P2** |
| **Architecture & Clean Code** | ⚠️ Manual mappings, non-standard package naming, dead code | MapStruct, domain validation, spotless/checkstyle, clean package structure | **P2 / P3** |

---

## 🚨 Phase 1: Critical Fixes, Data Integrity & Correctness (P0)

### 1.1 HTTP Status Codes & REST API Corrections
- [x] **Fix 404 Not Found on missing discount**: In `DiscountController.getByProductId` ([DiscountController.java:48](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/DiscountService/controller/DiscountController.java#L48)), replace `ResponseEntity.status(200).build()` with standard `404 Not Found`.
- [x] **Separate Single vs Bulk Endpoints**:
  - `POST /api/v1/discounts` for batch discount creation/update.
  - `PUT /api/v1/discounts/{productId}` for single discount update/upsert.
  - Return `201 Created`, `200 OK`, `204 No Content` appropriately.
- [x] **Implement DELETE endpoint**: Add `DELETE /api/v1/discounts/{productId}` to support discount removal.

### 1.2 Kafka Partition Key & Event Ordering
- [x] **Supply Message Key in Kafka Producer**: In `KafkaSender.sendDiscountChanges` ([KafkaSender.java:28](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/DiscountService/kafka/KafkaSender.java#L28)), pass `String.valueOf(discountChangesDto.productId())` as the Kafka message key.

### 1.3 Concurrency, Transactions & Atomic Upserts
- [x] **Add `@Transactional` boundaries**: In `DiscountService`, add `@Transactional` to ensure atomicity across batch database operations. Add `@Transactional(readOnly = true)` to read methods.
- [x] **Eliminate Check-Then-Act Race Condition**: Replace `findByProductId` followed by conditional `save`/`update` in `DiscountService` with an atomic PostgreSQL upsert query in `DiscountRepository`.
- [x] **Fix Batch Performance (N+1 Query Elimination)**: Batch query existing discounts and perform `batchUpsert` with `PreparedStatement` batching.
- [x] **Handle Duplicate Product IDs in Single Request**: Deduplicate incoming batch requests preserving latest entry to prevent self-conflicts within a single batch transaction.

### 1.4 Precision: Replace Floating Point with `BigDecimal`
- [x] **Migrate `double` to `BigDecimal`**:
  - In `DiscountRequest`, `DiscountResponse`, `DiscountChangesDto`, and `DiscountEntity`, replace `double` with `BigDecimal`.
  - In `V1__init_schema.sql` and `schema.sql`, alter `DOUBLE PRECISION` to `NUMERIC(10, 2)`.

---

## ⚡ Phase 2: Distributed Reliability & Resilience (P1)

### 2.1 Transactional Outbox Pattern (Dual-Write Prevention)
- [x] **Implement Outbox Table**: Created Flyway migration `V2__outbox_events.sql` (`id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `status`, `retry_count`, `error_message`, `created_at`, `processed_at`) with partial index on `(status, created_at) WHERE status = 'PENDING'`.
- [x] **Transactional Outbox Writer**: In `DiscountService`, atomic database operations write `OutboxEvent` records into `outbox_events` in the exact same transaction as the discounts table.
- [x] **Outbox Relay / Publisher (`FOR UPDATE SKIP LOCKED`)**:
  - Implemented `OutboxScheduler` with `@Scheduled` polling and `FOR UPDATE SKIP LOCKED` query in `OutboxRepository`.
  - Non-blocking row-level lock concurrency across multiple instances.
  - Synchronous broker delivery acknowledgment before marking status as `PROCESSED`, with retry count handling.

### 2.2 Kafka Producer Reliability Configuration
- [x] **Configure Producer Resilience Properties** in `application.yaml`:
  - `spring.kafka.producer.acks: all`
  - `spring.kafka.producer.properties.enable.idempotence: true`
  - `spring.kafka.producer.properties.retries: 2147483647`
  - `spring.kafka.producer.properties.max.in.flight.requests.per.connection: 5`
  - `spring.kafka.producer.properties.delivery.timeout.ms: 120000`
- [ ] **Standardize CloudEvents / Schema Registry**:
  - Wrap payloads in standard metadata envelope (e.g., CloudEvents standard: `eventId`, `eventType`, `eventTime`, `source`, `correlationId`, `dataVersion`, `data`).
  - Consider Avro / JSON Schema with Confluent Schema Registry for backward/forward schema compatibility.

### 2.3 Input Validation & Standardized Error Handling
- [x] **Add JSR 380 Validation Annotations**:
  - In `DiscountRequest`: `@NotNull`, `@Positive(message = "Product ID must be positive")`, `@DecimalMin(value = "0.00")`, `@DecimalMax(value = "100.00")`.
  - In `DiscountController`: Add `@Validated` to controller and `@Valid` / `@NotEmpty` to request parameters.
- [x] **Implement Global Exception Handler (`@RestControllerAdvice`)**:
  - Return RFC 7807 `ProblemDetail` responses for:
    - `MethodArgumentNotValidException` (400 Bad Request with field error map)
    - `ConstraintViolationException` (400 Bad Request)
    - `DiscountNotFoundException` (404 Not Found)
    - `DuplicateKeyException` / Optimistic locking failures (409 Conflict)
    - `Exception` (500 Internal Server Error with sanitized error messages)

### 2.4 Pagination & Query Optimization
- [x] **Add Pagination and Sorting to `GET /api/v1/discounts`**:
  - Add `GET /api/v1/discounts/paged?page=0&size=20` with `PageResponse<T>` metadata.
  - Optimize query with `LIMIT` and `OFFSET` in `DiscountRepository`.

---

## 🧪 Phase 3: Comprehensive Testing Strategy (P1)

### 3.1 Unit Testing (Currently 0% Unit Test Coverage)
- [ ] **`DiscountServiceTest`**: Test business logic, upsert logic, outbox event generation, single vs bulk operations, validation errors using Mockito (`@ExtendWith(MockitoExtension.class)`).
- [ ] **`DiscountControllerTest`**: Test HTTP routing, serialization, validation constraints, and error status codes using `@WebMvcTest(DiscountController.class)`.
- [ ] **`DiscountRepositoryTest`**: Test SQL mapping, constraints, batch operations using `@JdbcTest` with Testcontainers.
- [ ] **`KafkaSenderTest`**: Test message publishing, key hashing, and failure callbacks.

### 3.2 Integration & Edge Case Testing
- [ ] **Add Integration Tests for Failure Scenarios**:
  - Request with invalid discount values (e.g., negative discount, discount > 100).
  - Request with empty payload / malformed JSON.
  - `GET /api/v1/discounts/{productId}` for non-existent product returns 404.
  - Kafka broker failure handling.
  - Database rollback verification during partial batch failure.
- [ ] **Concurrency & Race Condition Tests**: Write multi-threaded integration tests with `ExecutorService` verifying atomic concurrent updates on the same product ID without duplicate key errors.

### 3.3 Test Infrastructure & Quality Gates
- [ ] **Remove Redundant `test/resources/schema.sql`**: Ensure integration tests rely exclusively on Flyway migrations to mirror production schema management.
- [ ] **Add ArchUnit Tests**: Validate architectural constraints (controllers don't access repositories directly, packages follow layer isolation, naming conventions).
- [ ] **Add JaCoCo Code Coverage Plugin**: Enforce minimum line (85%) and branch (80%) coverage thresholds in `pom.xml`.
- [ ] **Add Pitest (Mutation Testing)**: Validate test quality and assertion effectiveness.

---

## 🔭 Phase 4: Observability, Metrics & Telemetry (P1 / P2)

### 4.1 Actuator & Kubernetes Probes
- [x] **Expose Actuator Endpoints**:
  - In `application.yaml`, configure `management.endpoints.web.exposure.include: "health,info,metrics,prometheus"`.
  - Enable Kubernetes probes:
    ```yaml
    management:
      endpoint:
        health:
          probes:
            enabled: true
          show-details: when_authorized
      health:
        livenessstate:
          enabled: true
        readinessstate:
          enabled: true
    ```
- [ ] **Custom Health Indicators**: Add database and Kafka connectivity health checks.

### 4.2 Prometheus Metrics & Custom Meters
- [x] **Add `micrometer-registry-prometheus`** to `pom.xml`.
- [ ] **Register Custom Business Metrics**:
  - `discounts.created.count` (counter tagged by status)
  - `discounts.updated.count` (counter)
  - `discounts.value.percent` (distribution summary)
  - `kafka.publish.latency` (timer)

### 4.3 Structured Logging & Tracing
- [ ] **Create `logback-spring.xml`**:
  - Configure JSON logging format (e.g. `LogstashLogbackEncoder`) for production profile (ELK / Grafana Loki ingestion).
  - Keep human-readable colored output for local development profile.
- [ ] **Distributed Tracing & Correlation IDs**:
  - Migrate from Brave to OpenTelemetry (`micrometer-tracing-bridge-otel`).
  - Add `CorrelationIdFilter` / MDC interceptor to extract `X-Correlation-Id` / `X-Request-Id` from incoming HTTP headers and propagate downstream to Kafka headers and logs.

---

## 🔒 Phase 5: Security, Governance & Resilience (P1 / P2)

### 5.1 Security & Authentication
- [ ] **Add Spring Security / OAuth2 Resource Server**:
  - Protect write endpoints (`POST`, `PUT`, `DELETE`) with JWT / role-based authorization (e.g., `hasAuthority('SCOPE_discount:write')`).
  - Allow read endpoints (`GET`) for authenticated services / internal clients.
- [ ] **Configure Security Headers & CORS**:
  - Add CSP, HSTS, X-Frame-Options, X-Content-Type-Options headers.
  - Strict CORS policy specifying allowed origins rather than wildcard `*`.

### 5.2 Rate Limiting & Resilience
- [ ] **Add Rate Limiting (Bucket4j / Redis / Gateway)**: Protect bulk update and read endpoints against denial-of-service or burst overload.
- [ ] **Add Resilience4j**:
  - Circuit Breaker & Retry on external service calls (Eureka registration, remote clients).
  - Bulkhead configuration to isolate critical thread pools.

---

## 🏛️ Phase 6: Architecture, Database & Code Quality (P2)

### 6.1 Clean Code, Mappers & Java Conventions
- [x] **Rename Package**: Refactor package from `com.serjnn.DiscountService` (PascalCase violation) to standard `com.serjnn.discountservice`.
- [x] **Introduce MapStruct**: Replace manual object constructors with type-safe, compile-time MapStruct mappers (`DiscountMapper.java`).
- [x] **Clean Up Dead & Unused Code**:
  - Remove/align consumer configurations and resolve todo items.

### 6.2 Database Schema & Connection Pool Optimization
- [x] **Add Audit Fields and Versioning**:
  - Add `created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`
  - Add `updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`
- [x] **Add Database Constraints**:
  - Add check constraint: `CONSTRAINT check_discount_range CHECK (discount >= 0.00 AND discount <= 100.00)`
- [x] **Tune HikariCP Pool** in `application.yaml`:
  - `spring.datasource.hikari.maximum-pool-size: 20`
  - `spring.datasource.hikari.minimum-idle: 5`
  - `spring.datasource.hikari.idle-timeout: 300000`
  - `spring.datasource.hikari.connection-timeout: 20000`
  - `spring.datasource.hikari.leak-detection-threshold: 2000`

### 6.3 Domain Richness & Business Logic Extensions
- [ ] **Discount Validity Periods**: Add `start_date` and `end_date` (temporal validity for flash sales and scheduled campaigns).
- [ ] **Discount Types**: Support percentage discounts (`PERCENTAGE`) vs fixed monetary discounts (`FIXED_AMOUNT`).
- [ ] **Discount Tiering / Category Discounts**: Support category-level or customer-segment discounts in addition to product-level.

---

## 🚀 Phase 7: DevOps, CI/CD, Containerization & Tooling (P2 / P3)

### 7.1 Dockerfile Hardening
- [x] **Run Container as Non-Root User**:
  - In `Dockerfile`, create dedicated unprivileged user (`spring`) and switch with `USER spring:spring`.
- [x] **Add JVM Container Optimization Flags**:
  - Configure `JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"`.
- [x] **Add Container Healthcheck**:
  - Add `HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget --no-verbose --tries=1 --spider http://localhost:7005/actuator/health/liveness || exit 1`.

### 7.2 Local Development Environment (`docker-compose`)
- [x] **Create `docker-compose.yml`**:
  - PostgreSQL 15 container with initialized `discount_schema`.
  - Apache Kafka in KRaft mode.
  - Zipkin tracing backend.
  - Prometheus metrics scraper with `prometheus.yml`.
  - Discount Service app container.

### 7.3 Automated CI/CD Pipelines
- [x] **Add GitHub Actions Workflow (`.github/workflows/ci.yml`)**:
  - Matrix builds on Java 17 / 21.
  - Run `./mvnw clean verify` with Testcontainers.
  - Build Docker image.

### 7.4 Code Style, Formatting & Static Analysis
- [x] **Add `.editorconfig`**: Ensure consistent indentation, line endings (LF), and charset across IDEs.

### 7.5 Kubernetes & Helm Manifests
- [x] **Create Kubernetes Manifests (`k8s/`)**:
  - `Deployment` with CPU/Memory resource requests and limits, liveness & readiness probes.
  - `HorizontalPodAutoscaler` (HPA) based on CPU utilization.
  - `ConfigMap` for externalized service endpoints.
  - `Service` exposing ClusterIP on port 7005.

---

## 📚 Phase 8: Documentation & Developer Experience (P3)

### 8.1 API Documentation & Interactive UI
- [x] **Add SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)**:
  - Annotate endpoints with `@Operation`, `@ApiResponse`, `@Schema`.
  - Expose Swagger UI at `/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`.

### 8.2 Project Documentation & Governance
- [x] **Update `README.md`**:
  - Document all endpoints, pagination, Kafka events, environment variables, Docker Compose, and Kubernetes.
  - Include Mermaid architecture diagram.
- [x] **Add Standard Repository Governance**:
  - `CONTRIBUTING.md` (branching strategy, commit conventions).
  - `CHANGELOG.md` (Keep a Changelog standard).
  - `LICENSE` file (Apache 2.0).

