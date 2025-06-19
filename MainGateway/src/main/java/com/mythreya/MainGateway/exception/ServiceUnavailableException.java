package com.mythreya.MainGateway.exception;


public class ServiceUnavailableException extends GatewayException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}