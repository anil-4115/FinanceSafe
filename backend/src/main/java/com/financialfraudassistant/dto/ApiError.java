package com.financialfraudassistant.dto;

import java.util.Map;

public record ApiError(boolean success, String message, String errorCode, int status, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(false, message, statusCode(status), status, null);
    }

    public static ApiError of(int status, String message, String errorCode) {
        return new ApiError(false, message, errorCode, status, null);
    }

    public static ApiError validation(String message, Map<String, String> fieldErrors) {
        return new ApiError(false, message, "VALIDATION_FAILED", 400, fieldErrors);
    }

    private static String statusCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 500 -> "INTERNAL_SERVER_ERROR";
            default -> "ERROR";
        };
    }
}