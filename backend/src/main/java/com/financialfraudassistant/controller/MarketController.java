package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.MarketDetailResponse;
import com.financialfraudassistant.dto.MarketSearchResult;
import com.financialfraudassistant.service.MarketService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/search")
    public List<MarketSearchResult> search(Authentication authentication, @RequestParam(required = false) String q) {
        return marketService.search(q);
    }

    @GetMapping("/{symbol}")
    public MarketDetailResponse detail(Authentication authentication, @PathVariable String symbol) {
        return marketService.detail(symbol);
    }
}