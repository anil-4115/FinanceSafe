package com.financialfraudassistant.dto;

import com.financialfraudassistant.model.FinancialProduct;

import java.util.List;

public record ProductResponse(
        Integer id,
        String name,
        String category,
        String riskLevel,
        String expectedReturn,
        String liquidity,
        String minAmount,
        String tenure,
        String suitableFor,
        List<String> pros,
        List<String> cons,
        String description) {

    public static ProductResponse from(FinancialProduct product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getRiskLevel(),
                product.getExpectedReturn(),
                product.getLiquidity(),
                product.getMinAmount(),
                product.getTenure(),
                product.getSuitableFor(),
                split(product.getPros()),
                split(product.getCons()),
                product.getDescription());
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("\\R")).map(String::trim).filter(line -> !line.isBlank()).toList();
    }
}