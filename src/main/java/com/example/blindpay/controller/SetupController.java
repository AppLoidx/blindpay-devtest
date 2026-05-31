package com.example.blindpay.controller;

import com.example.blindpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SetupController {

    private final UserRepository userRepository;

    @GetMapping("/")
    public Map<String, Object> status() {
        long count = userRepository.count();
        if (count > 0) {
            return Map.of("status", "ready", "users", count,
                    "message", "Service is running. Use /api/users endpoints.");
        }
        return Map.of("status", "not_ready", "message", "No users found.");
    }
}
