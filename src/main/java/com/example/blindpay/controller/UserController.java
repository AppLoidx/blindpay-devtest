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
