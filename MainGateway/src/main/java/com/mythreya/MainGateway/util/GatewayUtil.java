package com.mythreya.MainGateway.util;




import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class GatewayUtil {
    public static boolean isPublicPath(String path, List<String> publicPaths) {
        return publicPaths.stream()
            .anyMatch(publicPath -> path.startsWith(publicPath));
    }

    public static Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(response.toString().getBytes())));
    }
}
