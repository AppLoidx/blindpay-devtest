package com.example.blindpay.controller;

import com.example.blindpay.dto.TransferRequest;
import com.example.blindpay.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransferController {

    private final UserService userService;

    @PostMapping("/transfer")
    public Map<String, Object> transfer(@RequestBody TransferRequest request) {
        log.info("POST /api/transfer — from: {}, to: {}, amount: {}",
                request.getFromUserId(), request.getToUserId(), request.getAmount());
        return userService.transfer(
                request.getFromUserId(), request.getToUserId(), request.getAmount());
    }
}
