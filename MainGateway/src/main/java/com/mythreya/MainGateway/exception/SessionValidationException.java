package com.mythreya.MainGateway.exception;

public class SessionValidationException extends GatewayException {
    public SessionValidationException(String message) {
        super(message);
    }

    public SessionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}