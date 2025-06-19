package com.example.NearMeBKND.qanda.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiResponse<T> {
    private T data;
    private String message;
    private boolean success;
    private String errorCode;
    private Map<String, List<String>> validationErrors;
    private String timestamp;
    private String requestPath;

    public ApiResponse() {
        this.validationErrors = new HashMap<>();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Success response with data
    public static <T> T success(T data) {
        return data;
    }

    // Success response with data and message
    public static <T> T success(T data, String message) {
        return data;
    }

    // Error response with message
    public static <T> T error(String message) {
        throw new RuntimeException(message);
    }

    // Error response with message and error code
    public static <T> T error(String message, String errorCode) {
        throw new RuntimeException(message);
    }

    // Validation error response
    public static <T> T validationError(Map<String, List<String>> errors) {
        throw new RuntimeException("Validation failed");
    }

    // Getters and Setters
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
} 