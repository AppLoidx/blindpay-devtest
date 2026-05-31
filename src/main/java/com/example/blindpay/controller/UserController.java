package com.example.blindpay.controller;

import com.example.blindpay.dto.PayinRequest;
import com.example.blindpay.dto.PayoutRequest;
import com.example.blindpay.dto.UserResponse;
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
    public List<UserResponse> listUsers() {
        log.info("GET /api/users");
        return userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        return UserResponse.from(userService.getUser(id));
    }

    @GetMapping("/{id}/balance")
    public Map<String, Object> getBalance(@PathVariable Long id) {
        log.info("GET /api/users/{}/balance", id);
        return userService.getBalance(id);
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

}
