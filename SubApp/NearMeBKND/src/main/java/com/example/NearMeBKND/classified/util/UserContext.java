package com.example.NearMeBKND.classified.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserContext {
    public static final String USER_ID_HEADER = "X-User-ID";

    public static String getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID header is required");
            }
            return userId;
        }
        throw new IllegalStateException("No request context found");
    }
} 