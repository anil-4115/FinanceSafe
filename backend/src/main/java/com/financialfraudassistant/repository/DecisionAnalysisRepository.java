package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.DecisionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DecisionAnalysisRepository extends JpaRepository<DecisionAnalysis, Integer> {
    List<DecisionAnalysis> findByUserIdOrderByCreatedAtDesc(Integer userId);
}