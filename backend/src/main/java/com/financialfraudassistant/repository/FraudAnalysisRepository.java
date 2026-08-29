package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.FraudAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, Integer> {
    List<FraudAnalysis> findByUserIdOrderByCreatedAtDesc(Integer userId);
    Optional<FraudAnalysis> findByIdAndUserId(Integer id, Integer userId);
}