# Tests, UI & PITest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add unit tests, a click-test UI, and PITest mutation testing to the BlindPay Test Service.

**Architecture:** Unit tests mock `BlindPayApiService` to test `UserService` logic and controller endpoints via MockMvc. A single static HTML page provides a browser UI for manual testing. PITest Maven plugin measures test quality via mutation coverage.

**Tech Stack:** JUnit 5, Mockito, MockMvc, spring-boot-starter-test, PITest, vanilla HTML/CSS/JS

**Spec:** `docs/superpowers/specs/2026-05-31-tests-ui-design.md`

---

## File Map

| File | Responsibility |
|------|----------------|
| `pom.xml` | Add spring-boot-starter-test + PITest plugin |
| `src/test/java/com/example/blindpay/service/UserServiceTest.java` | Unit tests for UserService |
| `src/test/java/com/example/blindpay/controller/UserControllerTest.java` | MockMvc tests for UserController + exception handling |
| `src/test/java/com/example/blindpay/controller/TransferControllerTest.java` | MockMvc tests for TransferController |
| `src/main/resources/static/index.html` | Click-test UI |

---

### Task 1: Add test dependency and PITest plugin to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add spring-boot-starter-test dependency and PITest plugin**

Add the test dependency inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```

Add the PITest plugin inside `<plugins>` (after spring-boot-maven-plugin):

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

- [ ] **Step 2: Verify dependencies resolve**

Run: `mvn dependency:resolve -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add spring-boot-starter-test and PITest plugin"
```

---

### Task 2: UserServiceTest

**Files:**
- Create: `src/test/java/com/example/blindpay/service/UserServiceTest.java`

- [ ] **Step 1: Create UserServiceTest.java**

```java
package com.example.blindpay.service;

import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlindPayApiService blindPayApi;

    @InjectMocks
    private UserService userService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .name("Alice Doe")
                .email("alice@example.com")
                .receiverId("re_alice")
                .bankAccountId("ba_alice")
                .walletId("bl_alice")
                .walletAddress("0xalice")
                .blockchainWalletId("bw_alice")
                .tosId("to_alice")
                .build();

        bob = User.builder()
                .id(2L)
                .name("Bob Smith")
                .email("bob@example.com")
                .receiverId("re_bob")
                .bankAccountId("ba_bob")
                .walletId("bl_bob")
                .walletAddress("0xbob")
                .blockchainWalletId("bw_bob")
                .tosId("to_bob")
                .build();
    }

    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alice Doe");
        assertThat(result.get(1).getName()).isEqualTo("Bob Smith");
    }

    @Test
    void getUser_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        User result = userService.getUser(1L);

        assertThat(result.getName()).isEqualTo("Alice Doe");
        assertThat(result.getWalletId()).isEqualTo("bl_alice");
    }

    @Test
    void getUser_missingId_throwsIllegalArgument() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 99");
    }

    @Test
    void payin_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.createPayinQuote("bl_alice", 10000))
                .thenReturn(Map.of("id", "pq_123"));
        when(blindPayApi.createPayin("pq_123"))
                .thenReturn(Map.of("id", "pi_456", "status", "processing"));

        Map<String, Object> result = userService.payin(1L, 10000);

        assertThat(result.get("id")).isEqualTo("pi_456");
        assertThat(result.get("status")).isEqualTo("processing");
        verify(blindPayApi).createPayinQuote("bl_alice", 10000);
        verify(blindPayApi).createPayin("pq_123");
    }

    @Test
    void payout_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.createPayoutQuote("ba_alice", 5000))
                .thenReturn(Map.of("id", "qt_789"));
        when(blindPayApi.createPayout("qt_789", "0xalice"))
                .thenReturn(Map.of("id", "po_abc", "status", "pending"));

        Map<String, Object> result = userService.payout(1L, 5000);

        assertThat(result.get("id")).isEqualTo("po_abc");
        assertThat(result.get("status")).isEqualTo("pending");
        verify(blindPayApi).createPayoutQuote("ba_alice", 5000);
        verify(blindPayApi).createPayout("qt_789", "0xalice");
    }

    @Test
    void transfer_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(blindPayApi.createTransferQuote("bl_alice", "0xbob", 3000))
                .thenReturn(Map.of("id", "tq_111"));
        when(blindPayApi.createTransfer("tq_111"))
                .thenReturn(Map.of("id", "tr_222", "status", "completed"));

        Map<String, Object> result = userService.transfer(1L, 2L, 3000);

        assertThat(result.get("id")).isEqualTo("tr_222");
        assertThat(result.get("status")).isEqualTo("completed");
        verify(blindPayApi).createTransferQuote("bl_alice", "0xbob", 3000);
        verify(blindPayApi).createTransfer("tq_111");
    }

    @Test
    void getBalance_callsGetWalletWithCorrectIds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.getWallet("re_alice", "bl_alice"))
                .thenReturn(Map.of("id", "bl_alice", "address", "0xalice"));

        Map<String, Object> result = userService.getBalance(1L);

        assertThat(result.get("id")).isEqualTo("bl_alice");
        verify(blindPayApi).getWallet("re_alice", "bl_alice");
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=UserServiceTest -q`
Expected: All 6 tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/blindpay/service/UserServiceTest.java
git commit -m "test: add UserServiceTest with 6 unit tests"
```

---

### Task 3: UserControllerTest (includes exception handling tests)

**Files:**
- Create: `src/test/java/com/example/blindpay/controller/UserControllerTest.java`

- [ ] **Step 1: Create UserControllerTest.java**

```java
package com.example.blindpay.controller;

import com.example.blindpay.exception.BlindPayApiException;
import com.example.blindpay.model.User;
import com.example.blindpay.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser() {
        return User.builder()
                .id(1L)
                .name("Alice Doe")
                .email("alice@example.com")
                .receiverId("re_alice")
                .bankAccountId("ba_alice")
                .walletId("bl_alice")
                .walletAddress("0xalice")
                .blockchainWalletId("bw_alice")
                .tosId("to_alice")
                .build();
    }

    @Test
    void listUsers_returns200WithArray() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(testUser()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alice Doe")))
                .andExpect(jsonPath("$[0].walletId", is("bl_alice")));
    }

    @Test
    void getUser_returns200WithUser() throws Exception {
        when(userService.getUser(1L)).thenReturn(testUser());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Doe")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    void getUser_notFound_returns400() throws Exception {
        when(userService.getUser(99L))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("User not found: 99")));
    }

    @Test
    void getBalance_returns200() throws Exception {
        when(userService.getBalance(1L))
                .thenReturn(Map.of("id", "bl_alice", "address", "0xalice"));

        mockMvc.perform(get("/api/users/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("bl_alice")));
    }

    @Test
    void payin_returns200() throws Exception {
        when(userService.payin(1L, 10000))
                .thenReturn(Map.of("id", "pi_456", "status", "processing"));

        mockMvc.perform(post("/api/users/1/payin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("pi_456")))
                .andExpect(jsonPath("$.status", is("processing")));
    }

    @Test
    void payout_returns200() throws Exception {
        when(userService.payout(1L, 5000))
                .thenReturn(Map.of("id", "po_abc", "status", "pending"));

        mockMvc.perform(post("/api/users/1/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("po_abc")));
    }

    @Test
    void blindPayApiError_returnsMatchingStatusCode() throws Exception {
        when(userService.getBalance(1L))
                .thenThrow(new BlindPayApiException(502, "{\"message\":\"gateway error\"}"));

        mockMvc.perform(get("/api/users/1/balance"))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.error", is("BlindPay API Error")))
                .andExpect(jsonPath("$.detail", is("{\"message\":\"gateway error\"}")));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl . -Dtest=UserControllerTest -q`
Expected: All 7 tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/blindpay/controller/UserControllerTest.java
git commit -m "test: add UserControllerTest with 7 MockMvc tests including error handling"
```

---

### Task 4: TransferControllerTest

**Files:**
- Create: `src/test/java/com/example/blindpay/controller/TransferControllerTest.java`

- [ ] **Step 1: Create TransferControllerTest.java**

```java
package com.example.blindpay.controller;

import com.example.blindpay.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void transfer_returns200() throws Exception {
        when(userService.transfer(1L, 2L, 5000))
                .thenReturn(Map.of("id", "tr_222", "status", "completed"));

        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":1,\"toUserId\":2,\"amount\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("tr_222")))
                .andExpect(jsonPath("$.status", is("completed")));

        verify(userService).transfer(1L, 2L, 5000);
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `mvn test -q`
Expected: All 14 tests pass (6 + 7 + 1).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/blindpay/controller/TransferControllerTest.java
git commit -m "test: add TransferControllerTest"
```

---

### Task 5: Run PITest mutation coverage

- [ ] **Step 1: Run PITest**

Run: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`
Expected: Report generated at `target/pit-reports/*/index.html`. Check mutation coverage score in terminal output.

- [ ] **Step 2: Commit (no code changes, just verify it works)**

No commit needed — this is a verification step.

---

### Task 6: Static HTML UI

**Files:**
- Create: `src/main/resources/static/index.html`

- [ ] **Step 1: Create index.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BlindPay Test Service</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #333; padding: 20px; }
        h1 { text-align: center; margin-bottom: 24px; color: #1a1a2e; }
        .cards { display: flex; gap: 20px; margin-bottom: 24px; flex-wrap: wrap; }
        .card { flex: 1; min-width: 300px; background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .card h2 { margin-bottom: 12px; color: #16213e; }
        .card .field { margin-bottom: 6px; font-size: 13px; color: #555; }
        .card .field span { font-weight: 600; color: #333; }
        .actions { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
        .action-row { display: flex; gap: 8px; align-items: center; }
        input[type="number"] { width: 120px; padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
        button { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; font-weight: 500; color: #fff; transition: opacity 0.2s; }
        button:hover { opacity: 0.85; }
        button:disabled { opacity: 0.5; cursor: not-allowed; }
        .btn-balance { background: #2196F3; }
        .btn-payin { background: #4CAF50; }
        .btn-payout { background: #FF9800; }
        .btn-transfer { background: #9C27B0; }
        .transfer-section { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 24px; }
        .transfer-section h2 { margin-bottom: 12px; color: #16213e; }
        .transfer-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
        select { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
        .response-panel { background: #1a1a2e; border-radius: 8px; padding: 20px; }
        .response-panel h2 { color: #eee; margin-bottom: 12px; }
        .response-panel pre { color: #0f0; font-size: 13px; white-space: pre-wrap; word-break: break-all; max-height: 400px; overflow-y: auto; }
        .loading { color: #ff0; }
        .error { color: #f44; }
        .label { font-size: 13px; color: #666; min-width: 40px; }
    </style>
</head>
<body>
    <h1>BlindPay Test Service</h1>

    <div class="cards" id="userCards">
        <div class="card"><p>Loading users...</p></div>
    </div>

    <div class="transfer-section">
        <h2>Transfer</h2>
        <div class="transfer-row">
            <span class="label">From:</span>
            <select id="transferFrom"></select>
            <span class="label">To:</span>
            <select id="transferTo"></select>
            <span class="label">Amount:</span>
            <input type="number" id="transferAmount" placeholder="cents" min="1">
            <button class="btn-transfer" onclick="doTransfer()">Transfer</button>
        </div>
    </div>

    <div class="response-panel">
        <h2>Response</h2>
        <pre id="responseOutput">Waiting for action...</pre>
    </div>

    <script>
        let users = [];

        async function loadUsers() {
            try {
                const res = await fetch('/api/users');
                users = await res.json();
                renderCards();
                renderTransferDropdowns();
            } catch (e) {
                showResponse({ error: 'Failed to load users', detail: e.message }, true);
            }
        }

        function renderCards() {
            const container = document.getElementById('userCards');
            container.innerHTML = users.map(u => `
                <div class="card">
                    <h2>${u.name}</h2>
                    <div class="field">Email: <span>${u.email}</span></div>
                    <div class="field">Receiver: <span>${u.receiverId}</span></div>
                    <div class="field">Wallet: <span>${u.walletId}</span></div>
                    <div class="field">Address: <span title="${u.walletAddress}">${u.walletAddress.substring(0, 12)}...</span></div>
                    <div class="field">Bank: <span>${u.bankAccountId}</span></div>
                    <div class="actions">
                        <div class="action-row">
                            <button class="btn-balance" onclick="doBalance(${u.id})">Check Balance</button>
                        </div>
                        <div class="action-row">
                            <input type="number" id="payin-${u.id}" placeholder="cents" min="1">
                            <button class="btn-payin" onclick="doPayin(${u.id})">Payin</button>
                        </div>
                        <div class="action-row">
                            <input type="number" id="payout-${u.id}" placeholder="cents" min="1">
                            <button class="btn-payout" onclick="doPayon(${u.id})">Payout</button>
                        </div>
                    </div>
                </div>
            `).join('');
        }

        function renderTransferDropdowns() {
            const fromSelect = document.getElementById('transferFrom');
            const toSelect = document.getElementById('transferTo');
            const options = users.map(u => `<option value="${u.id}">${u.name}</option>`).join('');
            fromSelect.innerHTML = options;
            toSelect.innerHTML = options;
            if (users.length > 1) toSelect.selectedIndex = 1;
        }

        function showResponse(data, isError = false) {
            const el = document.getElementById('responseOutput');
            el.className = isError ? 'error' : '';
            el.textContent = JSON.stringify(data, null, 2);
        }

        function showLoading(action) {
            const el = document.getElementById('responseOutput');
            el.className = 'loading';
            el.textContent = `${action}...`;
        }

        async function apiCall(method, url, body = null) {
            showLoading(`${method} ${url}`);
            try {
                const opts = { method, headers: { 'Content-Type': 'application/json' } };
                if (body) opts.body = JSON.stringify(body);
                const res = await fetch(url, opts);
                const data = await res.json();
                showResponse(data, !res.ok);
                return data;
            } catch (e) {
                showResponse({ error: e.message }, true);
            }
        }

        async function doBalance(userId) {
            await apiCall('GET', `/api/users/${userId}/balance`);
        }

        async function doPayin(userId) {
            const amount = parseInt(document.getElementById(`payin-${userId}`).value);
            if (!amount || amount <= 0) { showResponse({ error: 'Enter a valid amount' }, true); return; }
            await apiCall('POST', `/api/users/${userId}/payin`, { amount });
        }

        async function doPayon(userId) {
            const amount = parseInt(document.getElementById(`payout-${userId}`).value);
            if (!amount || amount <= 0) { showResponse({ error: 'Enter a valid amount' }, true); return; }
            await apiCall('POST', `/api/users/${userId}/payout`, { amount });
        }

        async function doTransfer() {
            const fromId = document.getElementById('transferFrom').value;
            const toId = document.getElementById('transferTo').value;
            const amount = parseInt(document.getElementById('transferAmount').value);
            if (!amount || amount <= 0) { showResponse({ error: 'Enter a valid amount' }, true); return; }
            if (fromId === toId) { showResponse({ error: 'Cannot transfer to same user' }, true); return; }
            await apiCall('POST', '/api/transfer', { fromUserId: parseInt(fromId), toUserId: parseInt(toId), amount });
        }

        loadUsers();
    </script>
</body>
</html>
```

- [ ] **Step 2: Verify the UI loads**

Run: `mvn -q spring-boot:run &` (if not already running), then open `http://localhost:8080/index.html` in a browser.
Expected: Two user cards with buttons, transfer section, response panel.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add click-test UI for BlindPay operations"
```

---

### Task 7: Final verification

- [ ] **Step 1: Run all unit tests**

Run: `mvn test -q`
Expected: All 14 tests pass.

- [ ] **Step 2: Run PITest mutation coverage**

Run: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`
Expected: HTML report at `target/pit-reports/*/index.html`. Check mutation score in terminal.

- [ ] **Step 3: Final commit if any changes**

```bash
git add -A
git commit -m "chore: final verification of tests, UI, and PITest"
```
