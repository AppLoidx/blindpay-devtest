# CLAUDE.md

## Project

BlindPay Test Service — Spring Boot wrapper around BlindPay crypto-fiat API. Demonstrates fiat→crypto→fiat flow between two pre-seeded users (Alice, Bob) on Polygon Amoy testnet. Experiment/test tool, not production.

## Architecture

Controller → Service → BlindPayApiService (curl) → BlindPay API. No auth layer.

- `BlindPayApiService` shells out to `curl` via `ProcessBuilder` — **Cloudflare blocks all Java HTTP clients** (TLS fingerprinting). Do not revert to RestClient/HttpClient/OkHttp.
- `RestClientConfig` exists but has no active beans — intentionally empty.
- `DataInitializer` (CommandLineRunner) seeds Alice & Bob on startup with hardcoded BlindPay resource IDs. H2 in-memory with `ddl-auto: create` — data resets every restart.
- Two BlindPay API path prefixes: `/v1/instances/{id}/` (direct, used for most calls) and `/v1/e/instances/{id}/` (embedded, used only for TOS URL generation). Most endpoints are receiver-scoped: `/v1/instances/{id}/receivers/{re_id}/bank-accounts|wallets|blockchain-wallets`.
- Amounts are integers in cents (10000 = $100.00).

## Tech Stack

- Java 21 (Corretto), Spring Boot 3.4.5, Maven
- Spring Web, Spring Data JPA, H2 (in-memory, runtime)
- Lombok, Jackson, httpclient5 (unused dep from earlier iteration)
- Testing: JUnit 5, Mockito, MockMvc, PITest 1.17.4
- Static analysis: ArchUnit 1.3.0, Checkstyle 10.21.4, PMD 7.3.0, SpotBugs 4.8.6.6 (FindSecBugs 1.13.0)
- No auth, no Spring Security, no profiles

## Critical Commands

```bash
mvn spring-boot:run                                          # Start on :8080
mvn test                                                     # 27 tests (15 unit + 12 ArchUnit)
mvn verify                                                   # Full build + static analysis
mvn test-compile org.pitest:pitest-maven:mutationCoverage    # Mutation report → target/pit-reports/*/index.html
mvn checkstyle:check                                         # Checkstyle only
mvn pmd:check                                                # PMD only
mvn spotbugs:check                                           # SpotBugs only
```

- UI: http://localhost:8080/index.html
- H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:blindpay, sa, no password)
- Status: http://localhost:8080/

## Conventions

- Package structure: `config/`, `model/`, `repository/`, `service/`, `controller/`, `dto/`, `exception/`, `bootstrap/`
- DTOs suffixed with `Request` (`PayinRequest`, `PayoutRequest`, `TransferRequest`). Error responses use `ApiErrorResponse`.
- Exception handling: `@RestControllerAdvice` in `GlobalExceptionHandler`. `BlindPayApiException` passes through BlindPay's HTTP status. `IllegalArgumentException` → 400.
- Constructor injection via Lombok `@RequiredArgsConstructor`. No field injection.
- Tests use `@MockitoBean` (not `@MockBean` — removed in Spring Boot 3.4). AssertJ for service tests, Hamcrest for MockMvc.
- Test naming: `methodName_condition_expectedResult`

## Gotchas

- **API key in application.yml** (`blindpay.api-key`). Not in env vars. Do not push to public repos.
- **curl must be on PATH** — `BlindPayApiService` depends on it at runtime.
- **Pre-seeded users are hardcoded** in `DataInitializer` with specific BlindPay resource IDs. Creating new users requires TOS acceptance + receiver creation via BlindPay API (see specs). TOS tokens are single-use per receiver.
- **BlindPay docs are inaccurate**: field names differ from actual API (`payment_rail`→`type`, `amount`→`request_amount`, `beneficiary_name`→`name`). Transfer quotes need `amount_reference`, `sender_token`, `receiver_token`, `receiver_network`. Upload is at `/v1/upload` with `bucket=onboarding`.
- **Network is polygon_amoy**, not base_sepolia (disabled for managed wallets). Payment rail is PIX, not ACH.
- **Dev payins may not auto-complete** — PIX payins on dev can stay in `processing` indefinitely.
- **httpclient5 dependency is unused** — leftover from an earlier attempt to bypass Cloudflare. Safe to remove.

## Code Rules

All rules are enforced at build time. `mvn verify` runs every check. Violations fail the build.

### ArchUnit (12 rules, run during test phase)

| Rule | What it checks |
|------|---------------|
| Controllers must not access Repositories directly | Package dependency: `..controller..` → `..repository..` |
| Controllers must not be annotated with @Service | Annotation check on controller classes |
| @Transactional must not be on Controllers or Repositories | Annotation check on controller/repository classes and methods |
| JPA @Entity must not appear in Controller dependencies | Dependency check: controllers → @Entity-annotated classes |
| Service classes must implement interfaces | Interface implementation check in `..service..` package |
| No @Autowired on fields (constructor injection only) | Field annotation check across all classes |
| Controllers must be annotated @RestController/@Controller | Annotation presence in `..controller..` package |
| Repositories must extend JpaRepository/CrudRepository | Type hierarchy check in `..repository..` package |
| Single Responsibility (max 8 project class dependencies) | Dependency count per class |
| service.impl classes must implement service interface | Package-to-interface enforcement |

### Checkstyle (Google style base, verify phase)

| Rule | Limit |
|------|-------|
| Max method length | 20 lines |
| Max file length | 200 lines |
| Max line length | 120 characters |
| No wildcard imports | AvoidStarImport |
| No tab characters | FileTabCharacter |

### PMD (verify phase)

| Rule | Limit |
|------|-------|
| No empty catch blocks | EmptyCatchBlock |
| No God classes | GodClass |
| No dead code | UnusedPrivateField, UnusedPrivateMethod, UnusedLocalVariable |
| No System.out.println | SystemPrintln |
| Cyclomatic complexity | max 10 per method |
| No duplicate string literals | max 3 occurrences |
| Max methods per class | 15 |
| Max fields per class | 10 |

### SpotBugs + FindSecBugs (verify phase)

Enabled categories: CORRECTNESS, BAD_PRACTICE, PERFORMANCE, SECURITY. Threshold: MEDIUM.

Suppressed false positives:
- COMMAND_INJECTION — curl/ProcessBuilder usage is by design (Cloudflare blocks Java HTTP clients)
- CRLF_INJECTION_LOGS — log parameters are internal state, not user input
- EI_EXPOSE_REP2 — Lombok-generated constructors in Spring-managed beans

ErrorProne was evaluated and removed — incompatible with Lombok on Java 21 (crashes on generated code). SpotBugs covers similar bug categories.

### Config files

- `checkstyle.xml` — Checkstyle ruleset (repo root)
- `pmd-rules.xml` — PMD ruleset (repo root)
- `spotbugs-exclude.xml` — SpotBugs exclusion filter (repo root)
- `src/test/java/com/example/blindpay/architecture/ArchitectureTest.java` — ArchUnit rules

## Out of Scope

- Do not add Spring Security or authentication — this is a test tool.
- Do not replace curl/ProcessBuilder with a Java HTTP client — Cloudflare will block it.
- Do not change pre-seeded user IDs without creating new BlindPay resources first.
- Do not add new BlindPay endpoints without testing field names via curl first — the docs lie.

## References

Design specs and implementation plans:
- `docs/superpowers/specs/2026-05-31-blindpay-test-service-design.md`
- `docs/superpowers/specs/2026-05-31-tests-ui-design.md`
- `docs/superpowers/plans/2026-05-31-blindpay-test-service.md`
- `docs/superpowers/plans/2026-05-31-tests-ui-pitest.md`

## Codebase Map

```
src/main/java/com/example/blindpay/
  controller/   — REST endpoints: SetupController, TransferController, UserController (payin/payout/transfer/setup)
  service/      — Business logic: UserService (user ops), BlindPayApiService (API orchestration), CurlHttpClient (HTTP via curl)
  repository/   — Data access: UserRepository (JPA, User entity)
  model/        — JPA entities: User
  dto/          — Request/response objects: PayinRequest, PayoutRequest, TransferRequest, UserResponse, ApiErrorResponse
  config/       — Configuration: BlindPayProperties (@ConfigurationProperties), RestClientConfig (empty, intentional)
  exception/    — Error handling: BlindPayApiException, GlobalExceptionHandler (@RestControllerAdvice)
  bootstrap/    — Startup: DataInitializer (seeds Alice & Bob via CommandLineRunner)

src/main/resources/
  application.yml — Single profile (default): server port, H2, BlindPay API key/URL/instanceId
  static/index.html — Simple test UI
```
