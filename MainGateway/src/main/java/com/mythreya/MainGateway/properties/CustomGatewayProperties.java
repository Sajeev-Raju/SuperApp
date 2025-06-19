package com.mythreya.MainGateway.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "custom.gateway")
@Data
public class CustomGatewayProperties {
    private String registrationServiceUrl;
    private List<String> publicPaths;
    private int rateLimit;
    private int sessionTimeoutMinutes;
}