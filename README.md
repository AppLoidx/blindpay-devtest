# BlindPay Test Service

Spring Boot wrapper around the BlindPay crypto-fiat API. Demonstrates fiat-to-crypto-to-fiat flow between two pre-seeded users (Alice, Bob) on Polygon Amoy testnet. This is an experiment and test tool, not production software.

## Architecture

```
Controller -> Service -> BlindPayApiService -> CurlHttpClient -> BlindPay API
```

`CurlHttpClient` shells out to `curl` via `ProcessBuilder`. Cloudflare blocks all Java HTTP clients (TLS fingerprinting), so this is by design. Do not replace with RestClient, HttpClient, or OkHttp.

H2 in-memory database resets on every restart. `DataInitializer` seeds Alice and Bob with hardcoded BlindPay resource IDs. No authentication layer.

## Tech Stack

- Java 21 (Corretto), Spring Boot 3.4.5, Maven
- Spring Web, Spring Data JPA, H2 (in-memory)
- Lombok, Jackson
- JUnit 5, Mockito, MockMvc, PITest 1.17.4
- ArchUnit, Checkstyle, PMD, SpotBugs (static analysis)

## Quick Start

```bash
mvn spring-boot:run                   # Start on :8080
mvn test                              # Unit tests + ArchUnit
mvn verify                            # Full build with static analysis
```

- Status: http://localhost:8080/
- UI: http://localhost:8080/index.html
- H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:blindpay, sa, no password)

Requires `curl` on PATH.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/{id}/balance` | Get wallet balance |
| POST | `/api/users/{id}/payin` | Fiat-to-crypto payin |
| POST | `/api/users/{id}/payout` | Crypto-to-fiat payout |
| POST | `/api/transfer` | Crypto-to-crypto transfer between users |

Request bodies for payin/payout: `{"amount": 10000}` (integer cents, 10000 = $100.00).

## Package Structure

```
com.example.blindpay
  bootstrap/      DataInitializer (seeds Alice, Bob)
  config/         BlindPayProperties, RestClientConfig
  controller/     SetupController, UserController, TransferController
  dto/            PayinRequest, PayoutRequest, TransferRequest, ApiErrorResponse
  exception/      BlindPayApiException, GlobalExceptionHandler
  model/          User (JPA entity)
  repository/     UserRepository
  service/        UserService, BlindPayApiService, CurlHttpClient
```

## Static Analysis

Four tools enforce code quality at build time. All run during `mvn verify`.

| Tool | What it enforces |
|------|-----------------|
| ArchUnit (12 rules) | Layering (controllers cannot access repositories), no field injection, no entities in controller return types, service interfaces, SRP |
| Checkstyle | Google style base, max 20-line methods, max 200-line files, no wildcard imports |
| PMD | No empty catches, no dead code, no System.out, cyclomatic complexity max 10, no duplicate literals |
| SpotBugs + FindSecBugs | Correctness, bad practices, performance, security bugs |

See `CODERULES.md` for the full rule-to-tool enforcement matrix.

## Code Statistics

### Current state (main)

| Metric | Value |
|--------|------:|
| Source files | 18 |
| Test files | 4 |
| Source LOC | 805 |
| Test LOC | 451 |
| Unit tests | 15 |
| Architecture tests | 12 |
| Total tests | 27 |

### Per-file breakdown

| File | LOC |
|------|----:|
| BlindPayApiService.java | 213 |
| CurlHttpClient.java | 157 |
| UserService.java | 88 |
| UserController.java | 59 |
| DataInitializer.java | 52 |
| GlobalExceptionHandler.java | 37 |
| User.java | 35 |
| TransferController.java | 29 |
| SetupController.java | 27 |
| BlindPayProperties.java | 18 |
| RestClientConfig.java | 17 |
| BlindPayApiException.java | 16 |
| ApiErrorResponse.java | 12 |
| BlindpayTestApplication.java | 12 |
| TransferRequest.java | 10 |
| PayoutRequest.java | 8 |
| PayinRequest.java | 8 |
| UserRepository.java | 7 |

## Refactor History

Two refactors have been applied to this codebase. The first extracted HTTP transport from the API service. The second added static analysis tooling and fixed all violations.

### Refactor 1: Extract CurlHttpClient

Commit `5ebdcce`. Separated HTTP transport from domain logic.

| Metric | Before | After | Change |
|--------|-------:|------:|-------:|
| Largest file (LOC) | 370 | 166 | -55% |
| Total source files | 17 | 18 | +1 |
| Total LOC | 777 | 770 | -7 |
| Responsibilities in BlindPayApiService | 2 | 1 | -1 |
| Duplicated try/catch blocks | 3 | 0 | -3 |

Before: `BlindPayApiService` contained both API domain logic and raw curl/ProcessBuilder execution (370 lines, cognitive complexity 11). After: domain logic stays in `BlindPayApiService` (166 lines), HTTP transport moves to `CurlHttpClient` (137 lines).

### Refactor 2: Static Analysis Enforcement

Branch `refactor/static-analysis-fixes` (pending review). Added ArchUnit, Checkstyle, PMD, SpotBugs. Fixed all violations.

| Metric | Before (main) | After (fix branch) | Change |
|--------|-------:|------:|-------:|
| Source files | 18 | 21 | +3 |
| Total LOC | 805 | 898 | +93 |
| Tests | 15 | 27 | +12 |
| Static analysis tools | 0 | 4 | +4 |
| Architecture rules enforced | 0 | 12 | +12 |
| Build quality gates | 0 | 3 | +3 |

Violations found and resolved:

| Tool | Found | Fixed | Suppressed (false positive) | Needs refactor |
|------|------:|------:|----------------------------:|---------------:|
| ArchUnit | 7 | 7 | 0 | 0 |
| SpotBugs | 12 | 0 | 12 | 0 |
| PMD | 5 | 4 | 0 | 1 |
| Checkstyle | 6 | 6 | 0 | 0 |
| Total | 30 | 17 | 12 | 1 |

Changes made on the fix branch:
- Controllers no longer access repositories directly (routed through services)
- Controllers return DTOs instead of JPA entities (new `UserResponse` record)
- Service classes implement interfaces (`UserServiceApi`, `BlindPayApi`)
- Long methods split into helpers (max 20 lines enforced)
- Duplicate string literals extracted to constants
- Error handling centralized via `withErrorHandling()` template in CurlHttpClient

### Combined delta (original to fix branch)

| Metric | Original (pre-refactor) | After both refactors |
|--------|-------:|------:|
| Largest file (LOC) | 370 | 200 |
| Total source files | 17 | 21 |
| Total LOC | 777 | 898 |
| Total tests | 15 | 27 |
| Architecture rules | 0 | 12 |
| Static analysis tools | 0 | 4 |
| Code smells | 1 | 0 |

## Constraints

- Do not replace curl/ProcessBuilder with a Java HTTP client. Cloudflare will block it.
- Do not add Spring Security. This is a test tool.
- Do not change pre-seeded user IDs without creating new BlindPay resources first.
- API key is in `application.yml`. Do not push to public repositories.
- BlindPay API docs are inaccurate on field names. Test via curl before adding new endpoints.
- Network is `polygon_amoy`. Payment rail is PIX.
