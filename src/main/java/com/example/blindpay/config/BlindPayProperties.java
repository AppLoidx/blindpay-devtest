package com.example.blindpay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "blindpay")
public class BlindPayProperties {

    private String apiKey;
    private String instanceId;
    private String baseUrl;
    private String tosId;
}
