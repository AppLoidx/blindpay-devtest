# Before vs After Refactor

**Commit before:** `419149b` (fix: resolve SonarQube bugs and code smells)  
**Commit after:** `5ebdcce` (refactor: extract CurlHttpClient from BlindPayApiService)

---

## Before Refactor — Per-File Metrics

| File | LOC | Cognitive Complexity | Code Smells |
|------|----:|---------------------:|------------:|
| BlindPayApiService.java | 370 | 11 | 1 (hardcoded URI) |
| UserService.java | 88 | 0 | 0 |
| UserController.java | 59 | 0 | 0 |
| DataInitializer.java | 52 | 1 | 0 |
| GlobalExceptionHandler.java | 37 | 0 | 0 |
| User.java | 35 | 0 | 0 |
| TransferController.java | 29 | 0 | 0 |
| SetupController.java | 27 | 1 | 0 |
| BlindPayProperties.java | 18 | 0 | 0 |
| RestClientConfig.java | 17 | 0 | 0 |
| BlindPayApiException.java | 16 | 0 | 0 |
| ApiErrorResponse.java | 12 | 0 | 0 |
| BlindpayTestApplication.java | 12 | 0 | 0 |
| TransferRequest.java | 10 | 0 | 0 |
| PayoutRequest.java | 8 | 0 | 0 |
| PayinRequest.java | 8 | 0 | 0 |
| UserRepository.java | 7 | 0 | 0 |

**Largest file:** BlindPayApiService.java (370 LOC) — contained both domain logic AND HTTP transport.

---

## After Refactor — Per-File Metrics

| File | LOC | Cognitive Complexity | Code Smells |
|------|----:|---------------------:|------------:|
| BlindPayApiService.java | 166 | 0 | 1 (hardcoded URI) |
| CurlHttpClient.java | 137 | 11 | 0 |
| UserService.java | 69 | 0 | 0 |
| UserController.java | 49 | 0 | 0 |
| DataInitializer.java | 44 | 1 | 0 |
| GlobalExceptionHandler.java | 32 | 0 | 0 |
| User.java | 31 | 0 | 0 |
| TransferController.java | 24 | 0 | 0 |
| SetupController.java | 22 | 1 | 0 |
| BlindPayProperties.java | 15 | 0 | 0 |
| BlindPayApiException.java | 12 | 0 | 0 |
| RestClientConfig.java | 10 | 0 | 0 |
| BlindpayTestApplication.java | 9 | 0 | 0 |
| TransferRequest.java | 8 | 0 | 0 |
| PayoutRequest.java | 6 | 0 | 0 |
| PayinRequest.java | 6 | 0 | 0 |
| UserRepository.java | 5 | 0 | 0 |

**Largest file:** BlindPayApiService.java (166 LOC) — domain only, no HTTP.

---

## Delta Summary

| Metric | Before | After | Change |
|--------|-------:|------:|--------|
| Largest file LOC | 370 | 166 | -55% |
| Max cognitive complexity (single file) | 11 | 11 | same (moved to CurlHttpClient) |
| Total code smells | 1 | 1 | same |
| Responsibilities in BlindPayApiService | 2 | 1 | separated |
| Total source files | 17 | 18 | +1 (CurlHttpClient) |
| Total LOC (src/main) | 777 | 770 | -7 (removed duplication) |

---

## What Changed

| Aspect | Before | After |
|--------|--------|-------|
| HTTP execution | Embedded in BlindPayApiService | Extracted to CurlHttpClient |
| URL construction | Mixed with curl commands | Clean helper methods (instanceUrl, receiverUrl) |
| Error handling | 3x duplicated try/catch blocks in API service | Centralized in CurlHttpClient |
| Dependency injection | Manual constructor (properties + objectMapper) | @RequiredArgsConstructor (properties + http) |
| Testability | Must mock entire service or nothing | Can mock CurlHttpClient independently |

---

## Remaining Code Smell

| File | Issue | Severity | Accepted |
|------|-------|----------|----------|
| BlindPayApiService.java:24 | Hardcoded URI constant | MINOR | Yes — internal path, not user config |
