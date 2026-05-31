# BlindPay Test Service

Test service wrapping BlindPay crypto-fiat API. Two pre-seeded users (Alice, Bob) with wallets on Polygon Amoy testnet.

## Quick Start

```bash
mvn spring-boot:run          # Start on :8080
mvn test                     # Run 15 unit tests
mvn test-compile org.pitest:pitest-maven:mutationCoverage  # Mutation testing
```

- UI: http://localhost:8080/index.html
- H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:blindpay, user: sa)

## Stack

Java 21, Spring Boot 3.4.5, Spring Data JPA, H2 in-memory, Lombok, Maven

## Architecture

### BlindPay API Communication

**Cloudflare blocks all Java HTTP clients** via TLS fingerprinting. `BlindPayApiService` uses `curl` via `ProcessBuilder` instead of RestClient/HttpClient. This is intentional — do not attempt to switch back to a Java HTTP client.

### API Paths

BlindPay has two API path prefixes:
- `/v1/instances/{id}/` — direct API, requires full KYC fields + `tos_id` in body (used for receivers)
- `/v1/e/instances/{id}/` — embedded flow, requires short-lived token from TOS acceptance (used for TOS URL generation only)

Most endpoints are receiver-scoped:
- `/v1/instances/{id}/receivers/{re_id}/bank-accounts`
- `/v1/instances/{id}/receivers/{re_id}/wallets`
- `/v1/instances/{id}/receivers/{re_id}/wallets/{wl_id}/balance`
- `/v1/instances/{id}/receivers/{re_id}/blockchain-wallets`

### Key Differences from BlindPay Docs

The official docs are often incomplete or use different field names than the actual API:
- `payment_rail` → `type`, `beneficiary_name` → `name` (bank accounts)
- `amount` → `request_amount`, need `currency_type`/`token`/`network` (quotes)
- Transfer quotes need `amount_reference`, `sender_token`, `receiver_token`, `receiver_network`
- Upload endpoint is at `/v1/upload` (not instance-scoped), needs `bucket=onboarding`
- TOS tokens (`to_xxx`) are single-use per receiver and expire quickly on `/e/` path but persist on direct path

### Network & Payment Rail

- **Network:** `polygon_amoy` (dev testnet). `base_sepolia` is disabled for managed wallets.
- **Payment rail:** `pix` (PIX). ACH had undocumented required fields.
- **Stablecoin:** `USDB` (dev token)

## Project Structure

```
service/
  BlindPayApiService.java   — All BlindPay API calls (via curl)
  UserService.java          — Business logic (payin/payout/transfer/balance)
controller/
  UserController.java       — GET /api/users, /balance, POST /payin, /payout
  TransferController.java   — POST /api/transfer
  SetupController.java      — GET / (status)
bootstrap/
  DataInitializer.java      — Seeds Alice & Bob with pre-existing BlindPay resource IDs
model/User.java             — JPA entity with BlindPay IDs
```

## Testing

- Unit tests mock `BlindPayApiService` — never hit real API
- `@MockitoBean` (not `@MockBean` — removed in Spring Boot 3.4)
- PITest report: `target/pit-reports/*/index.html`

## Configuration

All BlindPay config in `application.yml` under `blindpay.*`:
- `api-key`, `instance-id`, `base-url`, `tos-id`
- **Do not commit real API keys to public repos**

## Pre-seeded Users

Users are hardcoded in `DataInitializer` with existing BlindPay resource IDs. To reset, you need new TOS acceptances + new receivers via the BlindPay API (see `docs/superpowers/specs/` for the full setup flow).
