package com.financialfraudassistant.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(Integer id, String role, String content, LocalDateTime createdAt) {
}