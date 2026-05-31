package com.example.blindpay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * RestClient config is not used — Cloudflare blocks all Java HTTP clients
 * via TLS fingerprinting. BlindPayApiService uses curl via ProcessBuilder instead.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final BlindPayProperties properties;
}
