# BlindPay Test Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot REST API wrapping BlindPay API to demo fiat→crypto→fiat transfers between two users.

**Architecture:** Spring Boot 3.x app with H2 in-memory DB. RestClient calls BlindPay API. Bootstrap creates two users with all BlindPay entities on startup. REST endpoints expose payin/payout/transfer operations.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA, H2, RestClient, Lombok, Maven

**Spec:** `docs/superpowers/specs/2026-05-31-blindpay-test-service-design.md`

---

## File Map

| File | Responsibility |
|------|----------------|
| `pom.xml` | Maven dependencies and build config |
| `src/main/resources/application.yml` | Spring + BlindPay config |
| `src/main/java/com/example/blindpay/BlindpayTestApplication.java` | Spring Boot entry point |
| `src/main/java/com/example/blindpay/config/BlindPayProperties.java` | `@ConfigurationProperties` for blindpay.* |
| `src/main/java/com/example/blindpay/config/RestClientConfig.java` | RestClient bean + logging interceptor |
| `src/main/java/com/example/blindpay/model/User.java` | JPA entity |
| `src/main/java/com/example/blindpay/repository/UserRepository.java` | Spring Data JPA repository |
| `src/main/java/com/example/blindpay/service/BlindPayApiService.java` | All BlindPay HTTP calls |
| `src/main/java/com/example/blindpay/service/UserService.java` | Business logic for payin/payout/transfer |
| `src/main/java/com/example/blindpay/controller/UserController.java` | REST endpoints |
| `src/main/java/com/example/blindpay/bootstrap/DataInitializer.java` | CommandLineRunner: create receivers, wallets, etc. |
| `src/main/java/com/example/blindpay/dto/PayinRequest.java` | Payin request DTO |
| `src/main/java/com/example/blindpay/dto/PayoutRequest.java` | Payout request DTO |
| `src/main/java/com/example/blindpay/dto/TransferRequest.java` | Transfer request DTO |
| `src/main/java/com/example/blindpay/dto/ApiErrorResponse.java` | Error response DTO |
| `src/main/java/com/example/blindpay/exception/BlindPayApiException.java` | Custom exception for BlindPay errors |
| `src/main/java/com/example/blindpay/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` |

---

### Task 1: Scaffold Maven project

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>blindpay-test</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>blindpay-test</name>
    <description>BlindPay API test service</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Verify Maven resolves dependencies**

Run: `mvn dependency:resolve -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git init
git add pom.xml
git commit -m "chore: scaffold Maven project with Spring Boot 3.4.5"
```

---

### Task 2: Application entry point and configuration

**Files:**
- Create: `src/main/java/com/example/blindpay/BlindpayTestApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/example/blindpay/config/BlindPayProperties.java`

- [ ] **Step 1: Create application.yml**

```yaml
blindpay:
  api-key: 9rX4HcNvzJTLWxHsPeY6qc
  instance-id: in_YsCWHso6of2F
  base-url: https://api.blindpay.com/v1
  tos-id: ""

spring:
  application:
    name: blindpay-test
  datasource:
    url: jdbc:h2:mem:blindpay
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: false

logging:
  level:
    com.example.blindpay: DEBUG
```

- [ ] **Step 2: Create BlindPayProperties.java**

```java
package com.example.blindpay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "blindpay")
public class BlindPayProperties {

    private String apiKey;
    private String instanceId;
    private String baseUrl;
    private String tosId;
}
```

- [ ] **Step 3: Create BlindpayTestApplication.java**

```java
package com.example.blindpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlindpayTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlindpayTestApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify the app starts**

Run: `mvn spring-boot:run`
Expected: Application starts on port 8080, no errors. H2 console accessible at `/h2-console`. Kill with Ctrl+C.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/blindpay/BlindpayTestApplication.java \
        src/main/java/com/example/blindpay/config/BlindPayProperties.java \
        src/main/resources/application.yml
git commit -m "feat: add Spring Boot app with BlindPay config properties"
```

---

### Task 3: RestClient with logging interceptor

**Files:**
- Create: `src/main/java/com/example/blindpay/config/RestClientConfig.java`

- [ ] **Step 1: Create RestClientConfig.java**

```java
package com.example.blindpay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final BlindPayProperties properties;

    @Bean
    public RestClient blindPayRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .requestInterceptor(new LoggingInterceptor())
                .build();
    }

    @Slf4j
    static class LoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            logRequest(request, body);
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(request, response);
            return response;
        }

        private void logRequest(HttpRequest request, byte[] body) {
            log.info(">>> {} {}", request.getMethod(), request.getURI());
            if (body.length > 0) {
                log.info(">>> Body: {}", new String(body, StandardCharsets.UTF_8));
            }
        }

        private void logResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
            byte[] responseBody = response.getBody().readAllBytes();
            log.info("<<< {} {} — Status: {}",
                    request.getMethod(), request.getURI(), response.getStatusCode());
            if (responseBody.length > 0) {
                log.info("<<< Body: {}", new String(responseBody, StandardCharsets.UTF_8));
            }
        }
    }
}
```

**Important:** `BufferingClientHttpRequestFactory` wraps the response so the body can be read multiple times (once for logging, once for deserialization).

However, `response.getBody().readAllBytes()` in the interceptor consumes the stream. The `BufferingClientHttpRequestFactory` handles this by buffering the response body so it can be re-read. This is critical — without it, the RestClient would get an empty body after the interceptor logs it.

- [ ] **Step 2: Verify app still starts**

Run: `mvn spring-boot:run`
Expected: Starts without errors. Kill with Ctrl+C.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/blindpay/config/RestClientConfig.java
git commit -m "feat: add RestClient with verbose logging interceptor"
```

---

### Task 4: Domain model and repository

**Files:**
- Create: `src/main/java/com/example/blindpay/model/User.java`
- Create: `src/main/java/com/example/blindpay/repository/UserRepository.java`

- [ ] **Step 1: Create User.java**

```java
package com.example.blindpay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String receiverId;
    private String bankAccountId;
    private String blockchainWalletId;
    private String walletId;
    private String walletAddress;
    private String tosId;
}
```

- [ ] **Step 2: Create UserRepository.java**

```java
package com.example.blindpay.repository;

import com.example.blindpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
```

- [ ] **Step 3: Verify app starts and table is created**

Run: `mvn spring-boot:run`
Expected: Starts without errors, Hibernate logs show `create table users`. Kill with Ctrl+C.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/blindpay/model/User.java \
        src/main/java/com/example/blindpay/repository/UserRepository.java
git commit -m "feat: add User entity and JPA repository"
```

---

### Task 5: DTOs and exception handling

**Files:**
- Create: `src/main/java/com/example/blindpay/dto/PayinRequest.java`
- Create: `src/main/java/com/example/blindpay/dto/PayoutRequest.java`
- Create: `src/main/java/com/example/blindpay/dto/TransferRequest.java`
- Create: `src/main/java/com/example/blindpay/dto/ApiErrorResponse.java`
- Create: `src/main/java/com/example/blindpay/exception/BlindPayApiException.java`
- Create: `src/main/java/com/example/blindpay/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create request DTOs**

`PayinRequest.java`:
```java
package com.example.blindpay.dto;

import lombok.Data;

@Data
public class PayinRequest {
    private int amount; // cents: 10000 = $100.00
}
```

`PayoutRequest.java`:
```java
package com.example.blindpay.dto;

import lombok.Data;

@Data
public class PayoutRequest {
    private int amount; // cents: 10000 = $100.00
}
```

`TransferRequest.java`:
```java
package com.example.blindpay.dto;

import lombok.Data;

@Data
public class TransferRequest {
    private Long fromUserId;
    private Long toUserId;
    private int amount; // cents: 5000 = $50.00
}
```

- [ ] **Step 2: Create ApiErrorResponse and exception classes**

`ApiErrorResponse.java`:
```java
package com.example.blindpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private int status;
    private String error;
    private String detail;
}
```

`BlindPayApiException.java`:
```java
package com.example.blindpay.exception;

import lombok.Getter;

@Getter
public class BlindPayApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public BlindPayApiException(int statusCode, String responseBody) {
        super("BlindPay API error: HTTP " + statusCode + " — " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
```

`GlobalExceptionHandler.java`:
```java
package com.example.blindpay.exception;

import com.example.blindpay.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BlindPayApiException.class)
    public ResponseEntity<ApiErrorResponse> handleBlindPayApiException(BlindPayApiException ex) {
        log.error("BlindPay API error: {}", ex.getMessage());
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getStatusCode(),
                "BlindPay API Error",
                ex.getResponseBody()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        ApiErrorResponse error = new ApiErrorResponse(400, "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ApiErrorResponse error = new ApiErrorResponse(500, "Internal Server Error", ex.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/blindpay/dto/ \
        src/main/java/com/example/blindpay/exception/
git commit -m "feat: add DTOs and global exception handling"
```

---

### Task 6: BlindPayApiService — all BlindPay HTTP calls

**Files:**
- Create: `src/main/java/com/example/blindpay/service/BlindPayApiService.java`

This is the largest file. It wraps every BlindPay endpoint. All methods log inputs and outputs. All responses are returned as `Map<String, Object>` parsed from JSON — no need for typed response classes since we're exploring the API.

- [ ] **Step 1: Create BlindPayApiService.java**

```java
package com.example.blindpay.service;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.exception.BlindPayApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class BlindPayApiService {

    private final RestClient restClient;
    private final BlindPayProperties properties;
    private final ObjectMapper objectMapper;

    public BlindPayApiService(RestClient blindPayRestClient,
                              BlindPayProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = blindPayRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private String instancePath() {
        return "/instances/" + properties.getInstanceId();
    }

    // --- TOS ---

    public Map<String, Object> createTosUrl(String redirectUrl) {
        log.info("=== Creating TOS acceptance URL ===");
        Map<String, Object> body = Map.of(
                "idempotency_key", UUID.randomUUID().toString(),
                "redirect_url", redirectUrl
        );
        return post(instancePath() + "/tos", body);
    }

    // --- Receivers ---

    public Map<String, Object> createReceiver(String firstName, String lastName,
                                               String email, String tosId) {
        log.info("=== Creating receiver: {} {} ===", firstName, lastName);
        Map<String, Object> body = Map.of(
                "first_name", firstName,
                "last_name", lastName,
                "email", email,
                "tos_id", tosId
        );
        return post(instancePath() + "/receivers", body);
    }

    // --- Bank Accounts ---

    public Map<String, Object> createBankAccount(String receiverId,
                                                  String beneficiaryName,
                                                  String routingNumber,
                                                  String accountNumber) {
        log.info("=== Creating ACH bank account for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId,
                "payment_rail", "ach",
                "beneficiary_name", beneficiaryName,
                "routing_number", routingNumber,
                "account_number", accountNumber
        );
        return post(instancePath() + "/bank-accounts", body);
    }

    // --- Blockchain Wallets ---

    public Map<String, Object> createBlockchainWallet(String receiverId,
                                                       String walletAddress) {
        log.info("=== Creating blockchain wallet for receiver {} on Base Sepolia ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId,
                "address", walletAddress,
                "chain_id", 84532,
                "is_account_abstraction", true
        );
        return post(instancePath() + "/blockchain-wallets", body);
    }

    // --- Managed Wallets ---

    public Map<String, Object> createWallet(String receiverId) {
        log.info("=== Creating managed wallet for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId
        );
        return post(instancePath() + "/wallets", body);
    }

    // --- Payin Quotes ---

    public Map<String, Object> createPayinQuote(String walletId, int amount,
                                                 String paymentMethod, String stablecoin) {
        log.info("=== Creating payin quote: amount={}, wallet={} ===", amount, walletId);
        Map<String, Object> body = Map.of(
                "wallet_id", walletId,
                "amount", amount,
                "payment_method", paymentMethod,
                "stablecoin", stablecoin
        );
        return post(instancePath() + "/payin-quotes", body);
    }

    // --- Payins ---

    public Map<String, Object> createPayin(String payinQuoteId) {
        log.info("=== Executing payin with quote {} ===", payinQuoteId);
        Map<String, Object> body = Map.of(
                "payin_quote_id", payinQuoteId
        );
        return post(instancePath() + "/payins/evm", body);
    }

    // --- Payout Quotes ---

    public Map<String, Object> createPayoutQuote(String bankAccountId, int amount) {
        log.info("=== Creating payout quote: amount={}, bankAccount={} ===", amount, bankAccountId);
        Map<String, Object> body = Map.of(
                "bank_account_id", bankAccountId,
                "amount", amount
        );
        return post(instancePath() + "/quotes", body);
    }

    // --- Payouts ---

    public Map<String, Object> createPayout(String quoteId, String senderWalletAddress) {
        log.info("=== Executing payout with quote {}, sender={} ===", quoteId, senderWalletAddress);
        Map<String, Object> body = Map.of(
                "quote_id", quoteId,
                "sender_wallet_address", senderWalletAddress
        );
        return post(instancePath() + "/payouts/evm", body);
    }

    // --- Transfer Quotes ---

    public Map<String, Object> createTransferQuote(String walletId,
                                                    String receiverWalletAddress,
                                                    int amount) {
        log.info("=== Creating transfer quote: amount={}, from wallet={}, to={} ===",
                amount, walletId, receiverWalletAddress);
        Map<String, Object> body = Map.of(
                "wallet_id", walletId,
                "receiver_wallet_address", receiverWalletAddress,
                "amount", amount
        );
        return post(instancePath() + "/transfer-quotes", body);
    }

    // --- Transfers ---

    public Map<String, Object> createTransfer(String transferQuoteId) {
        log.info("=== Executing transfer with quote {} ===", transferQuoteId);
        Map<String, Object> body = Map.of(
                "transfer_quote_id", transferQuoteId
        );
        return post(instancePath() + "/transfers", body);
    }

    // --- Generic POST helper ---

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("-> POST {} | body: {}", path, requestJson);

            String responseJson = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("<- POST {} | response: {}", path, responseJson);

            Map<String, Object> result = objectMapper.readValue(
                    responseJson, new TypeReference<>() {});
            return result;
        } catch (RestClientResponseException ex) {
            log.error("BlindPay API error on POST {}: {} — {}",
                    path, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BlindPayApiException(
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error calling BlindPay API POST {}: {}", path, ex.getMessage(), ex);
            throw new RuntimeException("Failed to call BlindPay API: " + path, ex);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/blindpay/service/BlindPayApiService.java
git commit -m "feat: add BlindPayApiService with all API calls and verbose logging"
```

---

### Task 7: UserService — business logic

**Files:**
- Create: `src/main/java/com/example/blindpay/service/UserService.java`

- [ ] **Step 1: Create UserService.java**

```java
package com.example.blindpay.service;

import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BlindPayApiService blindPayApi;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public Map<String, Object> payin(Long userId, int amount) {
        User user = getUser(userId);
        log.info("=== PAYIN: user={} ({}), amount={} ===", user.getName(), userId, amount);

        log.info("Step 1: Creating payin quote...");
        Map<String, Object> quote = blindPayApi.createPayinQuote(
                user.getWalletId(), amount, "ach", "usdb");
        String quoteId = (String) quote.get("id");
        log.info("Payin quote created: {}", quoteId);

        log.info("Step 2: Executing payin...");
        Map<String, Object> payin = blindPayApi.createPayin(quoteId);
        log.info("Payin executed: {}", payin);

        return payin;
    }

    public Map<String, Object> payout(Long userId, int amount) {
        User user = getUser(userId);
        log.info("=== PAYOUT: user={} ({}), amount={} ===", user.getName(), userId, amount);

        log.info("Step 1: Creating payout quote...");
        Map<String, Object> quote = blindPayApi.createPayoutQuote(
                user.getBankAccountId(), amount);
        String quoteId = (String) quote.get("id");
        log.info("Payout quote created: {}", quoteId);

        log.info("Step 2: Executing payout...");
        Map<String, Object> payout = blindPayApi.createPayout(
                quoteId, user.getWalletAddress());
        log.info("Payout executed: {}", payout);

        return payout;
    }

    public Map<String, Object> transfer(Long fromUserId, Long toUserId, int amount) {
        User fromUser = getUser(fromUserId);
        User toUser = getUser(toUserId);
        log.info("=== TRANSFER: from={} to={}, amount={} ===",
                fromUser.getName(), toUser.getName(), amount);

        log.info("Step 1: Creating transfer quote...");
        Map<String, Object> quote = blindPayApi.createTransferQuote(
                fromUser.getWalletId(), toUser.getWalletAddress(), amount);
        String quoteId = (String) quote.get("id");
        log.info("Transfer quote created: {}", quoteId);

        log.info("Step 2: Executing transfer...");
        Map<String, Object> transfer = blindPayApi.createTransfer(quoteId);
        log.info("Transfer executed: {}", transfer);

        return transfer;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/blindpay/service/UserService.java
git commit -m "feat: add UserService with payin/payout/transfer logic"
```

---

### Task 8: REST controller

**Files:**
- Create: `src/main/java/com/example/blindpay/controller/UserController.java`

- [ ] **Step 1: Create UserController.java**

```java
package com.example.blindpay.controller;

import com.example.blindpay.dto.PayinRequest;
import com.example.blindpay.dto.PayoutRequest;
import com.example.blindpay.dto.TransferRequest;
import com.example.blindpay.model.User;
import com.example.blindpay.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> listUsers() {
        log.info("GET /api/users");
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        return userService.getUser(id);
    }

    @PostMapping("/{id}/payin")
    public Map<String, Object> payin(@PathVariable Long id,
                                     @RequestBody PayinRequest request) {
        log.info("POST /api/users/{}/payin — amount: {}", id, request.getAmount());
        return userService.payin(id, request.getAmount());
    }

    @PostMapping("/{id}/payout")
    public Map<String, Object> payout(@PathVariable Long id,
                                      @RequestBody PayoutRequest request) {
        log.info("POST /api/users/{}/payout — amount: {}", id, request.getAmount());
        return userService.payout(id, request.getAmount());
    }

    @PostMapping("/transfer")
    public Map<String, Object> transfer(@RequestBody TransferRequest request) {
        log.info("POST /api/users/transfer — from: {}, to: {}, amount: {}",
                request.getFromUserId(), request.getToUserId(), request.getAmount());
        return userService.transfer(
                request.getFromUserId(), request.getToUserId(), request.getAmount());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/blindpay/controller/UserController.java
git commit -m "feat: add UserController with REST endpoints"
```

---

### Task 9: Bootstrap — DataInitializer

**Files:**
- Create: `src/main/java/com/example/blindpay/bootstrap/DataInitializer.java`

This is the startup logic. If `blindpay.tos-id` is empty, it generates a TOS URL and stops. If set, it creates two users with all BlindPay entities.

- [ ] **Step 1: Create DataInitializer.java**

```java
package com.example.blindpay.bootstrap;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import com.example.blindpay.service.BlindPayApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BlindPayProperties properties;
    private final BlindPayApiService blindPayApi;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("  BlindPay Test Service — Bootstrap");
        log.info("========================================");

        if (properties.getTosId() == null || properties.getTosId().isBlank()) {
            log.info("No TOS ID configured. Generating TOS acceptance URL...");
            Map<String, Object> tos = blindPayApi.createTosUrl("http://localhost:8080");
            log.info("============================================================");
            log.info("  IMPORTANT: Open this URL in your browser to accept TOS:");
            log.info("  {}", tos.get("url"));
            log.info("  Then set blindpay.tos-id in application.yml and restart.");
            log.info("============================================================");
            return;
        }

        String tosId = properties.getTosId();
        log.info("Using TOS ID: {}", tosId);

        User alice = createUser("Alice", "Doe", "alice@example.com", tosId,
                "Jane Doe", "021000021", "123456789");
        User bob = createUser("Bob", "Smith", "bob@example.com", tosId,
                "Bob Smith", "021000021", "987654321");

        log.info("========================================");
        log.info("  Bootstrap complete!");
        log.info("  Alice: id={}, receiver={}, wallet={}", alice.getId(), alice.getReceiverId(), alice.getWalletId());
        log.info("  Bob:   id={}, receiver={}, wallet={}", bob.getId(), bob.getReceiverId(), bob.getWalletId());
        log.info("========================================");
    }

    private User createUser(String firstName, String lastName, String email,
                            String tosId, String beneficiaryName,
                            String routingNumber, String accountNumber) {
        log.info("--- Setting up user: {} {} ---", firstName, lastName);

        // 1. Create receiver
        log.info("Creating receiver...");
        Map<String, Object> receiver = blindPayApi.createReceiver(
                firstName, lastName, email, tosId);
        String receiverId = (String) receiver.get("id");
        log.info("Receiver created: {}", receiverId);

        // 2. Create bank account
        log.info("Creating bank account...");
        Map<String, Object> bankAccount = blindPayApi.createBankAccount(
                receiverId, beneficiaryName, routingNumber, accountNumber);
        String bankAccountId = (String) bankAccount.get("id");
        log.info("Bank account created: {}", bankAccountId);

        // 3. Create managed wallet (to get an address)
        log.info("Creating managed wallet...");
        Map<String, Object> wallet = blindPayApi.createWallet(receiverId);
        String walletId = (String) wallet.get("id");
        String walletAddress = (String) wallet.get("address");
        log.info("Managed wallet created: id={}, address={}", walletId, walletAddress);

        // 4. Create blockchain wallet using the managed wallet's address
        log.info("Creating blockchain wallet...");
        Map<String, Object> blockchainWallet = blindPayApi.createBlockchainWallet(
                receiverId, walletAddress);
        String blockchainWalletId = (String) blockchainWallet.get("id");
        log.info("Blockchain wallet created: {}", blockchainWalletId);

        // 5. Save to DB
        User user = User.builder()
                .name(firstName + " " + lastName)
                .email(email)
                .receiverId(receiverId)
                .bankAccountId(bankAccountId)
                .blockchainWalletId(blockchainWalletId)
                .walletId(walletId)
                .walletAddress(walletAddress)
                .tosId(tosId)
                .build();

        return userRepository.save(user);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/blindpay/bootstrap/DataInitializer.java
git commit -m "feat: add DataInitializer bootstrap with TOS + receiver + wallet setup"
```

---

### Task 10: First run — TOS acceptance

This is a manual step. The app needs a TOS ID before it can create users.

- [ ] **Step 1: Run the app without tos-id**

Run: `mvn spring-boot:run`
Expected: App starts, logs a TOS URL, and stops bootstrap. Look for a log line like:
```
IMPORTANT: Open this URL in your browser to accept TOS:
https://app.blindpay.com/...
```

- [ ] **Step 2: Accept TOS in browser**

Open the URL from the logs in a browser. Accept the terms. After redirect, capture the `tos_id` query parameter from the URL.

- [ ] **Step 3: Update application.yml with tos-id**

Edit `src/main/resources/application.yml`, set:
```yaml
blindpay:
  tos-id: "tos_XXXXXXXXXX"  # the actual value from step 2
```

- [ ] **Step 4: Restart the app**

Run: `mvn spring-boot:run`
Expected: Bootstrap runs fully — creates 2 receivers, 2 bank accounts, 2 managed wallets, 2 blockchain wallets. All logged verbosely.

- [ ] **Step 5: Test the endpoints**

Run these curl commands in another terminal:

```bash
# List users
curl -s http://localhost:8080/api/users | jq .

# Get Alice (id=1)
curl -s http://localhost:8080/api/users/1 | jq .

# Payin $100 for Alice
curl -s -X POST http://localhost:8080/api/users/1/payin \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}' | jq .

# Transfer $50 from Alice to Bob
curl -s -X POST http://localhost:8080/api/users/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromUserId": 1, "toUserId": 2, "amount": 5000}' | jq .

# Payout $50 for Bob
curl -s -X POST http://localhost:8080/api/users/2/payout \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000}' | jq .
```

- [ ] **Step 6: Commit the final tos-id**

```bash
git add src/main/resources/application.yml
git commit -m "chore: set TOS ID after acceptance"
```
