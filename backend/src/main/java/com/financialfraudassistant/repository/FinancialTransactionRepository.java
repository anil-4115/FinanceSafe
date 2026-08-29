package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Integer> {
    List<FinancialTransaction> findByUserIdOrderByTransactionDateDescIdDesc(Integer userId);
}
