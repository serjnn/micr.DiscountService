# Contributing to Discount Service

Thank you for your interest in contributing to the **Discount Service**! We welcome all contributions, bug reports, feature suggestions, and pull requests.

---

## 🛠️ Development Setup

### Prerequisites
- **Java 17+** (OpenJDK / Eclipse Temurin recommended)
- **Maven 3.9+** (or use the included `./mvnw` wrapper)
- **Docker Desktop** (required for Testcontainers and local multi-service testing)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/serjnn/micr.DiscountService.git
cd micr.DiscountService

# Build and run all tests (including Testcontainers integration tests)
./mvnw clean verify
```

---

## 🌿 Branching & Git Workflow

- `main` / `master` — Production-ready branch.
- Feature branches: `feat/<feature-name>` or `fix/<bug-name>`.
- Use **Conventional Commits** for commit messages:
  - `feat: ...` for new capabilities
  - `fix: ...` for bug fixes
  - `refactor: ...` for code refactoring
  - `test: ...` for test enhancements
  - `docs: ...` for documentation updates
  - `chore: ...` for build, dependencies, and configuration changes

---

## 🧪 Testing Guidelines

1. **Unit & Integration Tests**:
   - Write tests for any new endpoints, database queries, and business logic.
   - We utilize **Testcontainers** for PostgreSQL and Apache Kafka to ensure realistic testing without external mock fragility.
   - Run tests locally with `./mvnw test`.

2. **Code Style**:
   - Follow the standard Java conventions configured in `.editorconfig`.
   - Ensure imports are clean and package structure adheres to `com.serjnn.discountservice.*`.

---

## 📬 Submitting Pull Requests

1. Ensure the test suite passes completely (`./mvnw clean verify`).
2. Provide a clear title and description explaining what changed and why.
3. Link relevant issues or TODO milestones if applicable.
