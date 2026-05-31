package com.example.blindpay.controller;

import com.example.blindpay.exception.BlindPayApiException;
import com.example.blindpay.model.User;
import com.example.blindpay.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
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
