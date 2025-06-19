package com.example.NearMeBKND.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UserValidationInterceptor userValidationInterceptor;

    public WebConfig(UserValidationInterceptor userValidationInterceptor) {
        this.userValidationInterceptor = userValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userValidationInterceptor)
                .addPathPatterns("/api/questions/**");
    }
} 