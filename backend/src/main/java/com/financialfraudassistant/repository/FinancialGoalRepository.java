package com.financialfraudassistant.repository;

import com.financialfraudassistant.model.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Integer> {
    List<FinancialGoal> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
