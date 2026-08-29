package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.FinancialProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
public interface FinancialProductRepository extends JpaRepository<FinancialProduct, Integer> {
    List<FinancialProduct> findByCategoryIgnoreCaseOrderByRiskLevel(String category);
    List<FinancialProduct> findAllByOrderByCategoryAsc();
    List<FinancialProduct> findByCategoryIn(Collection<String> categories);
    long count();
}