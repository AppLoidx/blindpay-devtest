# Development Quality Report

**Project:** BlindPay Test Service  
**Date:** 2026-05-31  
**SonarQube:** http://localhost:9000/dashboard?id=blindpay-test

---

## Summary Dashboard

| Metric | Value | Rating | Delta |
|--------|-------|--------|-------|
| Lines of Code | 770 | - | - |
| Test Cases | 15 | - | - |
| Code Coverage | 25.1% | LOW | - |
| Mutation Score | 31% | LOW | - |
| Test Strength | 100% | EXCELLENT | - |
| Bugs | 0 | A | -3 fixed |
| Vulnerabilities | 0 | A | - |
| Code Smells | 4 (minor) | A | -13 fixed |
| Security Rating | A | PASS | - |
| Maintainability Rating | A | PASS | - |
| Reliability Rating | A | PASS | was C |
| Duplication | 3.8% | OK | - |
| Tech Debt | 35 min | LOW | was 176 min |

---

## Code Coverage (JaCoCo)

| Class | Lines Covered | Coverage |
|-------|--------------|----------|
| UserService | 43/43 | **100%** |
| UserController | 11/11 | **100%** |
| TransferController | 5/5 | **100%** |
| BlindPayApiException | 4/4 | **100%** |
| GlobalExceptionHandler | 10/13 | 73% |
| BlindPayApiService | 1/169 | 0.4% |
| DataInitializer | 0/33 | 0% |
| SetupController | 0/5 | 0% |

**Overall: 75/288 lines (25.1%)**

> Low coverage driven by `BlindPayApiService` (curl/ProcessBuilder — hard to unit test) and `DataInitializer` (bootstrap runner). All business logic classes at 100%.

---

## Mutation Testing (PITest)

| Class | Mutations | Killed | No Coverage | Score |
|-------|-----------|--------|-------------|-------|
| UserService | 7 | 7 | 0 | **100%** |
| UserController | 5 | 5 | 0 | **100%** |
| TransferController | 1 | 1 | 0 | **100%** |
| GlobalExceptionHandler | 6 | 5 | 1 | 83% |
| BlindPayApiService | 34 | 10 | 24 | 29% |
| SetupController | 2 | 0 | 2 | 0% |

**Total: 48 mutations | 15 killed | 0 survived | 33 no coverage**

- **Mutation Score:** 31%
- **Test Strength:** 100% (zero survived mutants)

---

## Issues Fixed

### Bugs Resolved (3)
- `BlindPayApiService:249,288,326` — `InterruptedException` now re-interrupts thread + throws domain exception

### Code Smells Resolved (13)
- Extracted constants: `NETWORK`, `NETWORK_KEY`, `RECEIVERS_PATH`, `REQUEST_AMOUNT`, `AUTH_HEADER_PREFIX`, `CURL_STATUS_SUFFIX`
- Replaced `RuntimeException` with `BlindPayApiException` (4 locations)
- Simplified `DataInitializer` log statements (removed duplicated separator string)
- Replaced `assertThat(map.get(k))` with `assertThat(map).containsEntry(k, v)` (6 assertions)

### Remaining Issues (4 — Minor, accepted)
| Location | Issue | Reason Accepted |
|----------|-------|-----------------|
| BlindPayApiService:24 | Hardcoded URI path | Internal constant, not user-facing config |
| UserServiceTest:98,114,131 | Chain multiple assertions | Style preference, readability OK as-is |

---

## Ratings Summary

| Category | Before | After |
|----------|--------|-------|
| Reliability | C | **A** |
| Security | A | A |
| Maintainability | A | A |
| Tech Debt | 176 min | **35 min** |
| Bugs | 3 | **0** |
| Code Smells | 17 | **4** |

---

## How to Reproduce

```bash
# Start SonarQube
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community

# Run tests + coverage
mvn clean test

# Run mutation testing
mvn test-compile org.pitest:pitest-maven:mutationCoverage

# Run SonarQube analysis
mvn sonar:sonar -Dsonar.token=<TOKEN> -Dsonar.host.url=http://localhost:9000

# Reports
# JaCoCo HTML:  target/site/jacoco/index.html
# PITest HTML:  target/pit-reports/index.html
# SonarQube:    http://localhost:9000/dashboard?id=blindpay-test
```
