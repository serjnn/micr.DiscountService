# Changelog: Transactional Outbox Pattern Refactoring & Hardening

**Date:** September 7, 2026  
**Module:** `com.serjnn.discountservice.outbox` & `com.serjnn.discountservice.service`  
**Status:** ✅ Completed & Verified by Integration Tests (11/11 Passing)

---

## 📌 1. Executive Summary

This refactoring resolves critical architectural vulnerabilities, performance bottlenecks, and reliability gaps in the **Transactional Outbox** pattern implementation of `micr.DiscountService`. 

Key enhancements include:
1. **Decoupling DB connection pools from Kafka remote network I/O** via an atomic lease claiming pattern.
2. **Implementing exponential retry backoff and non-blocking retry scheduling** to eliminate head-of-line blocking.
3. **Closing missing outbox event propagation on discount deletions**.
4. **Ensuring deterministic event ordering** across concurrent and batch-inserted events.
5. **Adding automated retention and periodic cleanup** for processed outbox entries.

---

## 🔍 2. Problems Identified & Solutions

| Area | Issue in Previous Version | Solution in Refactored Version |
|---|---|---|
| **DB Connection Pool Starvation** | `@Transactional` on `OutboxScheduler.processOutboxEvents()` held database connections and row locks during synchronous 5-second Kafka calls. | Removed `@Transactional` from scheduler loop. Replaced with atomic claim `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING ...` leasing events in `< 5ms`. |
| **Hot-Loop Retries & Head-of-Line Blocking** | Failed events remained `PENDING` with no delay, causing the scheduler to retry failing events every 500ms and blocking newer events behind them. | Added `next_retry_at` column and exponential backoff (`initialBackoffMs: 1000`, `backoffMultiplier: 2.0`, `maxBackoffMs: 60000`). |
| **Missing Outbox Events on Delete** | `DiscountService.deleteDiscount()` deleted DB rows without emitting an outbox event, leaving Kafka consumers unaware of deletions. | Added outbox event publication in `deleteDiscount()` with `newDiscount: 0.00` and `prevDiscount: <oldValue>`. |
| **Nondeterministic Batch Ordering** | Events in the same transaction shared the same `NOW()` timestamp and were queried with `ORDER BY created_at ASC`. | Updated query order to `ORDER BY created_at ASC, id ASC`. |
| **Unbounded Table Growth** | `outbox_events` accumulated records indefinitely without cleanup. | Added scheduled retention cleanup job `purgeOldProcessedEvents()` running daily at 02:00 (default retention: 7 days). |

---

## 📁 3. File-by-File Detailed Changes

### 1. [`V3__outbox_retry_and_ordering.sql`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/resources/db/migration/V3__outbox_retry_and_ordering.sql) *(New Flyway Migration)*
- Added column `next_retry_at TIMESTAMP WITH TIME ZONE`.
- Replaced single-column partial index with compound partial index:
  ```sql
  CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (next_retry_at, created_at, id) WHERE status = 'PENDING';
  ```

### 2. [`OutboxEvent.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/outbox/OutboxEvent.java)
- Extended `record OutboxEvent` with `Instant nextRetryAt`.
- Updated `OutboxEvent.pending(...)` factory method.

### 3. [`OutboxRepository.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/outbox/OutboxRepository.java)
- Added `claimPendingEvents(int limit, long leaseTimeMs)` using atomic PostgreSQL `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING ...`.
- Updated `markAsFailed(Long id, String errorMessage, int maxRetries, long backoffMs)` to compute and persist `next_retry_at`.
- Added `deleteOldProcessedEvents(int olderThanDays)` for retention cleanup.

### 4. [`OutboxScheduler.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/outbox/OutboxScheduler.java)
- Removed `@Transactional` method annotation to prevent holding database connections during network I/O.
- Integrated lease claiming, exponential backoff calculation, and error tracking.
- Added `@Scheduled` method `purgeOldProcessedEvents()` for automated background maintenance.

### 5. [`DiscountService.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/service/DiscountService.java)
- Updated `deleteDiscount(long productId)` to emit a `DiscountChangesDto` outbox event with `newDiscount: 0.00` and `prevDiscount: <previousDiscount>`.

### 6. [`application.yaml`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/resources/application.yaml) & [`application-test.yaml`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/test/resources/application-test.yaml)
- Added outbox scheduler tuning options:
  ```yaml
  outbox:
    scheduler:
      interval-ms: 500
      batch-size: 50
      max-retries: 5
      publish-timeout-ms: 5000
      lease-time-ms: 30000
      initial-backoff-ms: 1000
      max-backoff-ms: 60000
      backoff-multiplier: 2.0
    cleanup:
      cron: "0 0 2 * * *"
      retention-days: 7
  ```

### 7. [`schema.sql`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/test/resources/schema.sql)
- Aligned test schema definition with `next_retry_at` column and composite partial index.

### 8. [`DiscountIntegrationTest.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/test/java/com/serjnn/discountservice/DiscountIntegrationTest.java)
- Updated `shouldDeleteDiscountAndReturn404WhenNotFound` to assert Kafka message emission upon discount deletion.
- Added `shouldHandleFailedOutboxEventWithRetryAndBackoff` to verify resilient retry lifecycle and terminal `FAILED` status transition.

### 9. Type-Safe Configuration Properties as Records
- Created [`OutboxProperties.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/config/OutboxProperties.java) (`@ConfigurationProperties(prefix = "outbox")`) with nested record components `Scheduler` and `Cleanup`.
- Created [`KafkaTopicProperties.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/config/KafkaTopicProperties.java) (`@ConfigurationProperties(prefix = "spring.kafka.topic")`).
- Added `@ConfigurationPropertiesScan` to [`DiscountServiceApplication.java`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/DiscountServiceApplication.java).
- Refactored [`OutboxScheduler`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/outbox/OutboxScheduler.java), [`KafkaSender`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/kafka/KafkaSender.java), and [`KafkaTopicConfig`](file:///C:/Users/sersh/IdeaProjects/micr.DiscountService/src/main/java/com/serjnn/discountservice/kafka/KafkaTopicConfig.java) to inject typed property records, eliminating all scattered `@Value` annotations.
- Added `spring-boot-configuration-processor` to `pom.xml`.

---

## 🧪 4. Test Verification

```text
[INFO] Results:
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 01:32 min
[INFO] ------------------------------------------------------------------------
```
All integration tests passed using Testcontainers with **PostgreSQL 15** and **Kafka (confluentinc/cp-kafka:7.4.0)**.

