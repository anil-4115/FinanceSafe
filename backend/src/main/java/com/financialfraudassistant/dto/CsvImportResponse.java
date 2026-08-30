package com.financialfraudassistant.dto;

import java.util.List;

public record CsvImportResponse(int imported, List<String> errors, List<String> needsAttention) {
    public CsvImportResponse(int imported, List<String> needsAttention) {
        this(imported, needsAttention, needsAttention);
    }
}