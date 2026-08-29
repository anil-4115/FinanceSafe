package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.FraudIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FraudIndicatorRepository extends JpaRepository<FraudIndicator, Integer> {
    List<FraudIndicator> findByAnalysisId(Integer analysisId);
}