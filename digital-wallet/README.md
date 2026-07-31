# AI-Powered Digital Wallet

A Spring Boot monolith for managing digital-wallet balances and peer-to-peer transfers. The application uses PostgreSQL for durable wallet and transaction data, Redis for idempotency control, and Spring AI/OpenAI for transaction-note categorization.

## What it does

- Create a wallet and retrieve its balance or full details.
- Add mock funds to a wallet.
- Send money between wallets.
- Retrieve transaction history and individual transaction records.
- Prevent duplicate payment processing with an `Idempotency-Key`.
- Protect balance updates with database locks and atomic transactions.
- Persist both successful and failed transaction audit records.
- Categorize a transaction note with OpenAI (service integration; an HTTP endpoint is not yet exposed).

## Technology

| Area | Choice |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA + PostgreSQL |
| Cache / idempotency | Redis |
| AI | Spring AI + OpenAI |
| Build | Maven Wrapper |

## Architecture

The application is a layered Spring Boot monolith:

```text
REST controllers
      |
Service layer ── WalletService / TransactionService / AuditService / AiService
      |
PostgreSQL (wallets, transactions)     Redis (idempotency records)
```

`WalletService` owns all balance-changing workflows. `AuditService` runs with `REQUIRES_NEW` propagation, so failed transaction records are retained even when the wallet update rolls back.

## Consistency and concurrency guarantees

### Atomic transfers

Add-money and send-money operations run in a Spring transaction. In a transfer, the sender debit and receiver credit are committed together or rolled back together.

### Pessimistic wallet locks

Balance-changing operations load wallets with a pessimistic write lock (`SELECT ... FOR UPDATE`). Transfers lock the two wallets in sorted user-ID order, which reduces deadlock risk when opposite-direction transfers arrive simultaneously.

### Balance protection

Before a sender is debited, the service compares its `BigDecimal` balance with the requested amount. Insufficient funds produce a `400 Bad Request` response and no balance changes are committed.

### Idempotency

Provide an `Idempotency-Key` header for `add-money` and `send-money` requests. Redis stores a processing marker for up to two minutes and a successful response for 24 hours by default.

- A completed repeat request returns the original transaction/reference ID.
- A concurrent repeat request receives `409 Conflict` while the original is processing.
- A failed request removes its key, allowing a later retry.

Use a unique, non-empty key per logical payment. The current implementation expects the header to be supplied for payment requests.

### Audit trail

Successful transfers create a debit transaction for the sender and a credit transaction for the receiver under a shared reference prefix. When a workflow fails, its failed audit entry is written in an independent transaction.

## Prerequisites

- Java 21
- Docker and Docker Compose (recommended for PostgreSQL and Redis)
- An OpenAI API key if AI categorization is enabled

## Run locally

1. Start the infrastructure:

   ```bash
   docker compose up -d
   ```

2. Configure the application database URL in `src/main/resources/application.properties`.

   The included Compose file creates a database named `digitalwallet`, while the current application configuration uses `wallet`. Make these values match before starting. For the supplied Compose service, set:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/digitalwallet
   ```

3. Export the OpenAI key if you will use the AI service:

   ```bash
   export OPENAI_API_KEY="your-key"
   ```

4. Run the application:

   ```bash
   ./mvnw spring-boot:run
   ```

The server starts on `http://localhost:8080` by default.

## API reference

All successful responses use this shape:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

### Wallets

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/wallet/create?userId={userId}` | Create a zero-balance wallet. |
| `GET` | `/api/v1/wallet/{userId}` | Get a wallet. |
| `GET` | `/api/v1/wallet/balance/{userId}` | Get the current balance. |
| `POST` | `/api/v1/wallet/add-money` | Add mock funds. Requires `Idempotency-Key`. |
| `POST` | `/api/v1/wallet/send-money` | Transfer funds. Requires `Idempotency-Key`. |

Create wallets:

```bash
curl -X POST 'http://localhost:8080/api/v1/wallet/create?userId=alice'
curl -X POST 'http://localhost:8080/api/v1/wallet/create?userId=bob'
```

Add funds:

```bash
curl -X POST http://localhost:8080/api/v1/wallet/add-money \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: add-alice-001' \
  -d '{"userId":"alice","amount":1000.00}'
```

Send money:

```bash
curl -X POST http://localhost:8080/api/v1/wallet/send-money \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-alice-bob-001' \
  -d '{"senderId":"alice","receiverId":"bob","amount":250.00,"note":"Dinner payment"}'
```

### Transactions

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/transactions` | Create a mock transaction record. |
| `GET` | `/api/v1/transactions/user/{userId}` | Get a user’s transaction history. |
| `GET` | `/api/v1/transactions/{txnId}` | Get one transaction. |

Get Alice’s history:

```bash
curl http://localhost:8080/api/v1/transactions/user/alice
```

## AI categorization

`AiService` uses the prompt in `src/main/resources/prompts/category-prompt.st` to map a transaction note to one of the supported categories, such as `FOOD_DINING`, `TRANSPORT`, `BILLS_UTILITIES`, `RENT`, or `TRANSFER`. The configured client uses OpenAI through Spring AI.

The service currently exists as a backend integration and is not called by a controller or the transaction persistence flow. Natural-language spending questions (for example, “How much did I spend this month?”) and RAG-based transaction search are future extensions.

## Error responses

| Status | Typical cause |
| --- | --- |
| `400 Bad Request` | Duplicate wallet, invalid transfer state, or insufficient balance. |
| `404 Not Found` | Wallet or transaction does not exist. |
| `409 Conflict` | The same idempotency key is already being processed. |
| `500 Internal Server Error` | Unexpected server or infrastructure error. |

## Tests

The test suite includes wallet-service, idempotency, zero-balance boundary, fan-in concurrency, and advanced concurrency coverage.

```bash
./mvnw test
```

## Future scope

- Expose AI categorization through a secured API and persist the generated category.
- Add spending summaries and natural-language insight endpoints.
- Add RAG-based transaction search.
- Add authentication/authorization and user ownership checks.
- Add database indexes and API documentation (OpenAPI/Swagger).
- Replace mock funding with payment-provider integration.
