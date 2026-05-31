# BlindPay Test Service — Design Spec

## Overview

A Spring Boot REST API that wraps the BlindPay API to demonstrate the fiat→crypto→fiat flow. Two pre-seeded users each have a BlindPay receiver, ACH bank account, blockchain wallet (Base Sepolia), and managed wallet. The service exposes simple endpoints to trigger payins, payouts, and wallet-to-wallet transfers.

## Context

- **BlindPay API Key**: `9rX4HcNvzJTLWxHsPeY6qc`
- **Instance ID**: `in_YsCWHso6of2F`
- **Environment**: Development (auto-approved KYC, USDB test token, fiat steps auto-complete in ~30s)
- **Chain**: Base Sepolia (chain ID `84532`)
- **Stablecoin**: USDB
- **Payment Rail**: ACH (US)

## Tech Stack

- Java 21 (Corretto)
- Spring Boot 3.x (Web, Data JPA)
- H2 in-memory database
- RestClient (Spring 6.1+) for HTTP calls to BlindPay
- Lombok
- Maven

## Domain Model

### User (H2 entity)

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated PK |
| name | String | Display name |
| email | String | Email address |
| receiverId | String | BlindPay receiver ID (`re_xxx`) |
| bankAccountId | String | BlindPay bank account ID (`ba_xxx`) |
| blockchainWalletId | String | BlindPay blockchain wallet ID (`bw_xxx`) |
| walletId | String | BlindPay managed wallet ID (`bl_xxx`) |
| walletAddress | String | Blockchain wallet address |
| tosId | String | Accepted TOS ID |

## BlindPay API Integration

### Base URL

`https://api.blindpay.com/v1`

### Authentication

All requests include header: `Authorization: Bearer 9rX4HcNvzJTLWxHsPeY6qc`

### Endpoints Used

| BlindPay Endpoint | Method | Purpose |
|-------------------|--------|---------|
| `/v1/instances/{id}/tos` | POST | Generate TOS acceptance URL |
| `/v1/instances/{id}/receivers` | POST | Create receiver |
| `/v1/instances/{id}/bank-accounts` | POST | Add ACH bank account to receiver |
| `/v1/instances/{id}/blockchain-wallets` | POST | Add blockchain wallet to receiver |
| `/v1/instances/{id}/wallets` | POST | Create managed wallet |
| `/v1/instances/{id}/payin-quotes` | POST | Create payin quote |
| `/v1/instances/{id}/payins/evm` | POST | Execute payin |
| `/v1/instances/{id}/quotes` | POST | Create payout quote |
| `/v1/instances/{id}/payouts/evm` | POST | Execute payout |
| `/v1/instances/{id}/transfer-quotes` | POST | Create transfer quote |
| `/v1/instances/{id}/transfers` | POST | Execute transfer |

### Amount Format

All amounts are integers: `$100.00` → `10000`, `$1.23` → `123`.

## REST API

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users` | List both users with BlindPay IDs |
| `GET` | `/api/users/{id}` | Get single user details |
| `POST` | `/api/users/{id}/payin` | Fiat→crypto: create payin quote + execute |
| `POST` | `/api/users/{id}/payout` | Crypto→fiat: create payout quote + execute |
| `POST` | `/api/transfer` | Transfer USDB between user wallets |
| `GET` | `/api/users/{id}/balance` | Check user's managed wallet balance |

### Request/Response Examples

**POST /api/users/{id}/payin**
```json
// Request
{ "amount": 10000 }

// Response
{
  "payinId": "pi_xxx",
  "quoteId": "pq_xxx",
  "amountFiat": 10000,
  "status": "pending"
}
```

**POST /api/users/{id}/payout**
```json
// Request
{ "amount": 10000 }

// Response
{
  "payoutId": "po_xxx",
  "quoteId": "qt_xxx",
  "amountCrypto": 10000,
  "status": "pending"
}
```

**POST /api/transfer**
```json
// Request
{
  "fromUserId": 1,
  "toUserId": 2,
  "amount": 5000
}

// Response
{
  "transferId": "tr_xxx",
  "quoteId": "tq_xxx",
  "amount": 5000,
  "status": "completed"
}
```

## Bootstrap Flow (CommandLineRunner)

On application startup:

1. **TOS Acceptance** — call BlindPay to generate TOS URL. Log the URL. User must manually visit and accept in browser. The `tos_id` is captured (either hardcoded after first run, or passed via env var/config).
2. **Create Receivers** — POST two receivers (User A: "Alice", User B: "Bob") with the `tos_id`. Store `re_xxx` IDs.
3. **Add Bank Accounts** — POST ACH bank account for each receiver. Store `ba_xxx` IDs.
4. **Add Blockchain Wallets** — POST blockchain wallet for each (non-secure method: `is_account_abstraction: true`). Store `bw_xxx` and wallet addresses.
5. **Create Managed Wallets** — POST wallet for each receiver. Store `bl_xxx` IDs.
6. **Persist to H2** — Save all IDs to the User entities.

If TOS has already been accepted (tos_id in config), steps 2-6 run automatically. Otherwise, the app logs the TOS URL and waits for the user to configure the tos_id and restart.

## Configuration (application.yml)

```yaml
blindpay:
  api-key: 9rX4HcNvzJTLWxHsPeY6qc
  instance-id: in_YsCWHso6of2F
  base-url: https://api.blindpay.com/v1
  tos-id: ""  # Set after first TOS acceptance

spring:
  datasource:
    url: jdbc:h2:mem:blindpay
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create
```

## Logging

### RestClient Interceptor

A `ClientHttpRequestInterceptor` logs every BlindPay API call:
- **Request**: method, full URL, request body (JSON)
- **Response**: status code, response body (JSON)
- Log level: `INFO`

### Service-Level Logging

Each business operation logs:
- Step description (e.g., "Creating receiver for Alice")
- Input parameters
- Returned BlindPay object (ID, status, relevant fields)
- Errors with full response body

Logger names follow class names for easy filtering.

## Error Handling

- BlindPay API errors (4xx/5xx) are caught and returned as structured JSON error responses with the BlindPay error details.
- A global `@RestControllerAdvice` handles exceptions and returns consistent error format.

## Project Structure

```
src/main/java/com/example/blindpay/
  BlindpayTestApplication.java
  config/
    BlindPayProperties.java          # @ConfigurationProperties
    RestClientConfig.java            # RestClient bean + logging interceptor
  model/
    User.java                        # JPA entity
  repository/
    UserRepository.java              # Spring Data JPA
  service/
    BlindPayApiService.java          # All BlindPay API calls
    UserService.java                 # Business logic
  controller/
    UserController.java              # REST endpoints
  bootstrap/
    DataInitializer.java             # CommandLineRunner for setup
  dto/
    PayinRequest.java
    PayoutRequest.java
    TransferRequest.java
    ApiErrorResponse.java
  exception/
    BlindPayApiException.java
    GlobalExceptionHandler.java
```

## Testing Approach

Manual testing via curl/Postman. The dev instance auto-completes payins in ~30 seconds, so the flow can be verified end-to-end.

Test amounts:
- Normal: any amount (e.g., `10000` = $100.00)
- Failure test: `66600` ($666.00 → failed status)
- Refund test: `77700` ($777.00 → refunded status)
