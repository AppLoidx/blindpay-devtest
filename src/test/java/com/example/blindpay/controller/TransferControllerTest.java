package com.example.blindpay.controller;

import com.example.blindpay.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
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
