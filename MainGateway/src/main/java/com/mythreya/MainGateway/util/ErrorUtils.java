package com.mythreya.MainGateway.util;



import com.mythreya.MainGateway.exception.GatewayException;
import com.mythreya.MainGateway.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ErrorUtils {
    public static Mono<Void> handleError(ServerWebExchange exchange, 
                                       HttpStatus status, 
                                       String message, 
                                       Throwable ex) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().toString())
                .requestId(exchange.getRequest().getId())
                .build();

        byte[] bytes = errorResponse.toString().getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes)));
    }
}