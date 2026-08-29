package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.FinancialProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, Integer> {
    Optional<FinancialProfile> findByUserId(Integer userId);
}
