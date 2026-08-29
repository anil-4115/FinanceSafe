package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.ProductCompareResponse;
import com.financialfraudassistant.dto.ProductResponse;
import com.financialfraudassistant.model.FinancialProduct;
import com.financialfraudassistant.repository.FinancialProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final FinancialProductRepository productRepository;

    public ProductService(FinancialProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> list(String category) {
        List<FinancialProduct> products = category == null || category.isBlank()
                ? productRepository.findAllByOrderByCategoryAsc()
                : productRepository.findByCategoryIgnoreCaseOrderByRiskLevel(category);
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse detail(Integer id) {
        FinancialProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return ProductResponse.from(product);
    }

    public ProductCompareResponse compare(List<Integer> ids) {
        if (ids == null || ids.size() < 2 || ids.size() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select 2 or 3 products to compare");
        }
        List<FinancialProduct> products = new ArrayList<>();
        for (Integer id : ids) {
            products.add(productRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found")));
        }
        List<String> guidance = buildGuidance(products);
        return new ProductCompareResponse(products.stream().map(ProductResponse::from).toList(), guidance);
    }

    private List<String> buildGuidance(List<FinancialProduct> products) {
        List<String> guidance = new ArrayList<>();
        FinancialProduct first = products.get(0);
        FinancialProduct second = products.get(1);
        if (second.getRiskLevel().equalsIgnoreCase("HIGH") && !first.getRiskLevel().equalsIgnoreCase("HIGH")) {
            guidance.add(second.getName() + " carries higher risk; " + first.getName() + " may be more suitable if capital protection matters to you.");
        } else if (first.getRiskLevel().equalsIgnoreCase("HIGH")) {
            guidance.add(first.getName() + " may suit a higher risk-tolerant investor, while " + second.getName() + " offers more stability.");
        }
        if (first.getLiquidity().contains("High") && second.getLiquidity().contains("Low")) {
            guidance.add(first.getName() + " may be more suitable if you need quick access to your money.");
        } else if (second.getLiquidity().contains("High") && first.getLiquidity().contains("Low")) {
            guidance.add(second.getName() + " may be more suitable if liquidity is important to you.");
        }
        if (first.getTenure().contains("year") && second.getTenure().contains("year")) {
            long firstYears = digits(first.getTenure());
            long secondYears = digits(second.getTenure());
            if (firstYears > 0 && secondYears > 0 && firstYears < secondYears) {
                guidance.add(first.getName() + " may be more suitable for a shorter time frame; " + second.getName() + " rewards a longer lock-in.");
            }
        }
        if (products.size() == 3) {
            FinancialProduct third = products.get(2);
            guidance.add("Among all three, compare effective costs, exit penalties and tax treatment before choosing.");
        }
        if (guidance.isEmpty()) {
            guidance.add("The products are comparable; choose on liquidity, risk and expectations that fit your goal.");
        }
        guidance.add("This comparison is educational and not an endorsement. Do your own research or consult a SEBI-registered advisor before investing.");
        return guidance;
    }

    private long digits(String value) {
        try { return Long.parseLong(value.replaceAll("\\D", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }
}