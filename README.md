# Discount Service

## Overview
The **Discount Service** is a Spring Boot microservice responsible for managing product discounts. It allows for the creation, update, and retrieval of discounts while notifying other services of these changes via Kafka.

## Core Responsibilities
- **Discount Management**: Maintain a registry of discounts per product.
- **Change Notifications**: Publish events to Kafka when discounts are added or updated.
- **Service Discovery**: Register itself with a Eureka Service Registry for high availability and load balancing.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.2
- **Database**: PostgreSQL (with Flyway for migrations)
- **Messaging**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **Persistence**: Spring JDBC
- **Testing**: JUnit 5, Testcontainers (PostgreSQL, Kafka), Awaitility

---

## API Documentation

### Base URL: `/api/v1/discounts`

| Method | Endpoint | Description | Request Body | Response Body |
|--------|----------|-------------|--------------|---------------|
| `GET`  | `/`      | Get all discounts | None | `List<DiscountResponse>` |
| `POST` | `/`      | Create/Update discounts | `List<DiscountRequest>` | `201 Created` |
| `GET`  | `/{productId}` | Get discount for product | None | `DiscountResponse` or `404 Not Found` |

### Data Models

#### `DiscountRequest` / `DiscountResponse`
```json
{
  "productId": 123,
  "discount": 15.5
}
```

---

## Messaging (Kafka)

The service acts as a **Producer**, sending updates to the following topic:

### Topic: `discountChangesTopic`
- **Partitions**: 3
- **Payload**: `DiscountChangesDto`
- **When**: Triggered whenever a discount is successfully saved or updated in the database.

#### `DiscountChangesDto` Structure
```json
{
  "productId": 123,
  "newDiscount": 15.5,
  "prevDiscount": 10.0
}
```
*Note: For new discounts, `prevDiscount` will be `0.0`.*

---

## Configuration & Deployment

### Environment Variables / Properties
Configurable in `src/main/resources/application.yaml`:

- `server.port`: Default `7005`
- `spring.datasource.url`: Database connection string.
- `spring.kafka.bootstrap-servers`: Kafka broker address.
- `eureka.client.service-url.defaultZone`: Eureka server URL.

### Running the Service
1. Ensure PostgreSQL and Kafka are running.
2. Build the project:
   ```bash
   ./mvnw clean install
   ```
3. Start the service:
   ```bash
   ./mvnw spring-boot:run
   ```

### Database Migrations
Flyway is used for schema management. Migrations are located in `src/main/resources/db/migration/`.
The schema consists of a `discounts` table:
- `id` (Serial Primary Key)
- `product_id` (BigInt, Unique)
- `discount` (Numeric)
