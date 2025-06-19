package com.example.NearMeBKND.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.example.NearMeBKND.qanda.interceptor.UserIdValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired(required = false)
    private UserIdValidationInterceptor userIdValidationInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("X-User-ID")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (userIdValidationInterceptor != null) {
            registry.addInterceptor(userIdValidationInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/api/location/**")
                    .excludePathPatterns("/api/qanda/questions")
                    .excludePathPatterns("/api/meetups")
                    .excludePathPatterns("/api/classifieds")
                    .excludePathPatterns("/api/polls")
                    .excludePathPatterns("/**", "OPTIONS");
        }
    }
} 