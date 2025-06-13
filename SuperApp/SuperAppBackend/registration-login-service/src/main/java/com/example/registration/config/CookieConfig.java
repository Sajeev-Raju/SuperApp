package com.example.registration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CookieConfig {

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.max-age}")
    private int cookieMaxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Add your frontend URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
            HttpHeaders.SET_COOKIE
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public int getCookieMaxAge() {
        return cookieMaxAge;
    }
} 