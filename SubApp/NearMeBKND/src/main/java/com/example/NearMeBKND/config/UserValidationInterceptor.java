package com.example.NearMeBKND.config;

import com.example.NearMeBKND.util.UserValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserValidationInterceptor implements HandlerInterceptor {
    private final UserValidationUtil userValidationUtil;

    public UserValidationInterceptor(UserValidationUtil userValidationUtil) {
        this.userValidationUtil = userValidationUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("X-User-ID");
        if (userId == null || userId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User ID is required in headers");
            return false;
        }

        if (!userValidationUtil.isUserExists(userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User not found in the system");
            return false;
        }

        return true;
    }
} 