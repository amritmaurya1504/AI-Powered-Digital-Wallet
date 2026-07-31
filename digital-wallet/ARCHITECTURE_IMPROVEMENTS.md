# Digital Wallet: Architecture Improvements and Microservices Plan

## Current foundation

The application already has a strong monolith foundation:

- PostgreSQL for durable wallet and transaction data.
- Redis-backed idempotency control.
- Pessimistic locking for concurrent balance changes.
- Separate audit records for successful and failed transactions.
- Spring AI/OpenAI integration for transaction categorization.

The recommended path is an incremental migration to microservices, rather than splitting every feature at once.

## New service: User and Authentication Service

Create a `user-service` responsible for user identity and profile data. The wallet service should retain only a `userId`; it should not own credentials or profile information.

### Responsibilities

- Registration, login, password hashing, and password reset.
- JWT/OAuth2 authentication and token issuance.
- User profiles: name, email, phone number, and address.
- KYC/verification status.
- Roles such as `USER`, `ADMIN`, and `SUPPORT`.
- User account status: active, blocked, and verified.
- Publish a `UserRegistered` event so the wallet service can create a wallet.

## Target architecture

```text
Client
  |
API Gateway ---- User/Auth Service ---- user_db
  |
  +---- Wallet Service --------------- wallet_db
  +---- Transaction/Ledger Service --- ledger_db
  +---- Notification Service ---------- notification_db
  +---- AI Insights Service ----------- analytics/vector store
```

### API Gateway

The gateway is the public entry point. It should provide routing, JWT validation, rate limiting, request correlation IDs, and centralized CORS/security policy.

### Wallet Service

Owns wallet state, available balance, holds, limits, and balance-changing commands such as top-ups and transfers.

### Transaction/Ledger Service

Owns immutable debit and credit entries, transaction history, statements, and reconciliation. An immutable ledger makes financial records auditable and more reliable than using a mutable balance column as the complete source of truth.

### Notification Service

Consumes events to send email, SMS, or push notifications for transfers, failed payments, login alerts, low-balance warnings, and verification updates.

### AI Insights Service

Owns transaction categorization, spending summaries, budget insights, and natural-language queries. It must not participate in payment execution or make balance-changing decisions.

## Migration approach

Use a strangler migration so the existing monolith remains usable while services are extracted.

1. Organize the monolith into internal modules: `user`, `wallet`, `ledger`, `notification`, and `ai`.
2. Extract `user-service` first and use it to issue JWTs.
3. Add an API Gateway to route requests and validate tokens.
4. Keep the existing app as `wallet-service`, then extract transaction/ledger functionality.
5. Use asynchronous domain events for non-critical cross-service actions.
6. Give every service its own database or schema. Services must not query each other's tables directly.

Useful domain events include:

- `UserRegistered`
- `WalletCreated`
- `MoneyTransferred`
- `TransactionFailed`
- `UserBlocked`

For reliable event delivery, use the transactional outbox pattern: save the business update and an outbox event within one database transaction, then publish the event through Kafka or RabbitMQ. This prevents a committed balance change from losing its corresponding event or notification.

## Improvements to make before or during extraction

### Correctness and financial safety

- Require and validate the `Idempotency-Key` header for all payment-changing APIs.
- Mark an idempotency request as completed only after the database transaction has committed. A Redis completed record must never be written for a transaction that later rolls back.
- Define `BigDecimal` scale and rounding rules, and add a `currency` field to financial records.
- Return `404 Not Found` for an unknown wallet instead of returning a zero balance.
- Use a shared transfer reference ID and immutable debit/credit ledger entries for every transfer.
- Add transfer limits, velocity checks, fraud rules, and manual account blocking.

### API and validation

- Add `@Valid` to request bodies and validation constraints for user IDs, amounts, notes, and currencies.
- Return API DTOs rather than exposing JPA entities directly from controllers.
- Version and document APIs with OpenAPI/Swagger.
- Add ownership checks so authenticated users can access only their own wallet and transaction history.

### Persistence and infrastructure

- Add database indexes for `wallet.user_id`, transaction sender/receiver IDs, reference ID, status, and creation time.
- Replace `spring.jpa.hibernate.ddl-auto=update` with Flyway or Liquibase migrations.
- Fix the local configuration mismatch: Docker Compose creates `digitalwallet`, while the application currently points to `wallet`.
- Add health checks, readiness checks, structured logs, metrics, distributed traces, and correlation IDs.
- Use secrets management for database credentials and OpenAI keys rather than committing secrets to configuration.

### Testing and delivery

- Add integration tests using PostgreSQL and Redis containers.
- Add contract tests between gateway, user service, and wallet service.
- Add load/concurrency tests for transfer and idempotency behavior.
- Build CI/CD that runs formatting, unit tests, integration tests, security scans, and container image builds.

## Recommended first milestone

Build the following first:

1. `user-service` with profile management, login, and JWT authentication.
2. Secure the existing application as `wallet-service` with JWT ownership checks.
3. Add an API Gateway.
4. Run PostgreSQL per service, Redis, and optionally Kafka through Docker Compose.
5. Create wallets from a `UserRegistered` event.

This creates a practical microservices foundation while preserving the existing wallet implementation and avoiding unnecessary early service fragmentation.
