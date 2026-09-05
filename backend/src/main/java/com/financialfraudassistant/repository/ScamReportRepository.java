package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.ScamReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScamReportRepository extends JpaRepository<ScamReport, Integer> {
    List<ScamReport> findByUserIdOrderByCreatedAtDesc(Integer userId);
}