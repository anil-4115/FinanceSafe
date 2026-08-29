package com.financialfraudassistant.repository;

import com.financialfraudassistant.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    List<Budget> findByUserIdOrderByCategory(Integer userId);
    Optional<Budget> findByIdAndUserId(Integer id, Integer userId);
}
