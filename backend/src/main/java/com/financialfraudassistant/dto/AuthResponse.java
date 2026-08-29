package com.financialfraudassistant.dto;

public record AuthResponse(String token, String tokenType, long expiresIn, UserSummary user) {
    public record UserSummary(Integer id, String fullName, String email) { }
}
