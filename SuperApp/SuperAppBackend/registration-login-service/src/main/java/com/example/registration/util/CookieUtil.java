package com.example.registration.util;

import com.example.registration.config.CookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final CookieConfig cookieConfig;

    public void setCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieConfig.isCookieSecure());
        cookie.setDomain(cookieConfig.getCookieDomain());
        cookie.setPath("/");
        cookie.setMaxAge(cookieConfig.getCookieMaxAge());
        response.addCookie(cookie);
    }

    public void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieConfig.isCookieSecure());
        cookie.setDomain(cookieConfig.getCookieDomain());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
} 