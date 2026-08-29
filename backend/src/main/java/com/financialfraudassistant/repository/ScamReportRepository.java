package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.ScamReport;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScamReportRepository extends JpaRepository<ScamReport, Integer> { }
