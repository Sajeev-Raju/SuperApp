package com.mythreya.MainGateway.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.mythreya.MainGateway.model.ApiResponse;
import com.mythreya.MainGateway.model.SessionValidationRequest;
import com.mythreya.MainGateway.util.GatewayUtil;


import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SessionValidationFilter implements GlobalFilter, Ordered {
    private final WebClient.Builder webClientBuilder;
    private final List<String> publicPaths = Arrays.asList(
        "/api/register/start",
        "/api/register/verify-otp",
        "/api/register/validate-username",
        "/api/register/initiate-payment",
        "/api/register/complete",
        "/api/login/send-otp",
        "/api/login/verify",
        "/api/login/continue-with-oldest-logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        
        if (GatewayUtil.isPublicPath(path, publicPaths)) {
            return chain.filter(exchange);
        }

        String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-ID");
        String username = exchange.getRequest().getHeaders().getFirst("X-Username");

        if (sessionId == null || username == null) {
            return GatewayUtil.unauthorized(exchange, "Authentication required");
        }

        return validateSession(username, sessionId)
            .flatMap(isValid -> {
                if (isValid) {
                    return chain.filter(exchange);
                }
                return GatewayUtil.unauthorized(exchange, "Invalid or expired session");
            });
    }

    private Mono<Boolean> validateSession(String username, String sessionId) {
        SessionValidationRequest request = new SessionValidationRequest(username, sessionId);
        return webClientBuilder.build()
            .post()
            .uri("http://registration-service/api/session/validate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ApiResponse.class)
             .map(response -> {
    Map<String, Object> data = (Map<String, Object>) response.getData();
    return (Boolean) data.get("valid");
});

    }

    @Override
    public int getOrder() {
        return -1;
    }
}
