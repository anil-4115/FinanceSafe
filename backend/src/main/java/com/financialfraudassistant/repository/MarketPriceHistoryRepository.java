package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.MarketPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MarketPriceHistoryRepository extends JpaRepository<MarketPriceHistory, Integer> {
    List<MarketPriceHistory> findByAssetIdOrderByPriceDateAsc(Integer assetId);
    long countByAssetId(Integer assetId);
}