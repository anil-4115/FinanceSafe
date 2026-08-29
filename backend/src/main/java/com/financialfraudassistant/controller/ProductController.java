package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.ProductCompareResponse;
import com.financialfraudassistant.dto.ProductResponse;
import com.financialfraudassistant.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list(Authentication authentication, @RequestParam(required = false) String category) {
        return productService.list(category);
    }

    @GetMapping("/compare")
    public ProductCompareResponse compare(Authentication authentication, @RequestParam("ids") List<Integer> ids) {
        return productService.compare(ids);
    }

    @GetMapping("/{id}")
    public ProductResponse detail(Authentication authentication, @PathVariable Integer id) {
        return productService.detail(id);
    }
}