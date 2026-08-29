package com.financialfraudassistant.dto;

import java.util.List;

public record ProductCompareResponse(List<ProductResponse> products, List<String> guidance) { }