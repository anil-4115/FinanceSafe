package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AssetRepository extends JpaRepository<Asset, Integer> {
    List<Asset> findAllByOrderBySymbolAsc();
    java.util.Optional<Asset> findBySymbolIgnoreCase(String symbol);
}