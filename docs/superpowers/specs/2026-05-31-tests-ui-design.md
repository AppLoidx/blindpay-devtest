# Tests, UI & Mutation Testing — Design Spec

## Overview

Add unit tests, a simple click-test UI, and PITest mutation testing to the existing BlindPay Test Service.

## 1. Unit Tests

**Framework:** JUnit 5 + Mockito + MockMvc (from `spring-boot-starter-test`).

### UserServiceTest

Mock `BlindPayApiService` and `UserRepository`. Test:
- `getAllUsers()` — returns list from repository
- `getUser(id)` — returns user, throws `IllegalArgumentException` for missing
- `payin(userId, amount)` — calls `createPayinQuote` then `createPayin`, returns result
- `payout(userId, amount)` — calls `createPayoutQuote` then `createPayout`, returns result
- `transfer(fromId, toId, amount)` — calls `createTransferQuote` then `createTransfer`, returns result
- `getBalance(userId)` — calls `getWallet` with correct receiverId and walletId

### UserControllerTest

`@WebMvcTest(UserController.class)` with `@MockBean UserService`. Test:
- `GET /api/users` — 200, returns JSON array
- `GET /api/users/1` — 200, returns user JSON
- `GET /api/users/1/balance` — 200, returns wallet JSON
- `POST /api/users/1/payin` — 200, returns payin response
- `POST /api/users/1/payout` — 200, returns payout response

### TransferControllerTest

`@WebMvcTest(TransferController.class)` with `@MockBean UserService`. Test:
- `POST /api/transfer` — 200, returns transfer response

### GlobalExceptionHandlerTest

Test via MockMvc (can be in UserControllerTest):
- `BlindPayApiException` → returns matching status code + error body
- `IllegalArgumentException` → returns 400

### Files

- `src/test/java/com/example/blindpay/service/UserServiceTest.java`
- `src/test/java/com/example/blindpay/controller/UserControllerTest.java`
- `src/test/java/com/example/blindpay/controller/TransferControllerTest.java`

## 2. UI

Single file: `src/main/resources/static/index.html`

Served automatically by Spring Boot at `/index.html` (and also at `/` if no conflicting controller — we have SetupController on `/`, so UI will be at `/index.html` or we update SetupController).

### Layout

- **Header** — "BlindPay Test Service"
- **User Cards** (2 side-by-side) — loaded from `GET /api/users` on page load
  - Name, email, receiver ID, wallet ID, wallet address
  - "Check Balance" button
  - Amount input + "Payin" button
  - Amount input + "Payout" button
- **Transfer Section** — from/to user dropdowns, amount input, "Transfer" button
- **Response Panel** — shows raw JSON from last API call, auto-scrolls

### Tech

- Vanilla HTML/CSS/JS
- `fetch()` for API calls
- No frameworks, no build tools

## 3. Mutation Testing (PITest)

### Maven Plugin

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.17.4</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <targetClasses>
            <param>com.example.blindpay.service.*</param>
            <param>com.example.blindpay.controller.*</param>
            <param>com.example.blindpay.exception.*</param>
        </targetClasses>
        <targetTests>
            <param>com.example.blindpay.*</param>
        </targetTests>
        <outputFormats>
            <param>HTML</param>
        </outputFormats>
    </configuration>
</plugin>
```

### Run Command

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Report at `target/pit-reports/index.html`.
