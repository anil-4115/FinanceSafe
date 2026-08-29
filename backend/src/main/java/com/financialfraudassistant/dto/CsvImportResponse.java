package com.financialfraudassistant.dto;

import java.util.List;

public record CsvImportResponse(int imported, List<String> errors) { }