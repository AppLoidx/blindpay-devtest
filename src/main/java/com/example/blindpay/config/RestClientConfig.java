package com.example.blindpay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final BlindPayProperties properties;

    @Bean
    public RestClient blindPayRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .requestInterceptor(new LoggingInterceptor())
                .build();
    }

    @Slf4j
    static class LoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            logRequest(request, body);
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(request, response);
            return response;
        }

        private void logRequest(HttpRequest request, byte[] body) {
            log.info(">>> {} {}", request.getMethod(), request.getURI());
            if (body.length > 0) {
                log.info(">>> Body: {}", new String(body, StandardCharsets.UTF_8));
            }
        }

        private void logResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
            byte[] responseBody = response.getBody().readAllBytes();
            log.info("<<< {} {} — Status: {}",
                    request.getMethod(), request.getURI(), response.getStatusCode());
            if (responseBody.length > 0) {
                log.info("<<< Body: {}", new String(responseBody, StandardCharsets.UTF_8));
            }
        }
    }
}
