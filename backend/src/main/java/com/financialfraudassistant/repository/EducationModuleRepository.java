package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.EducationModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EducationModuleRepository extends JpaRepository<EducationModule, Integer> {
    List<EducationModule> findAllByOrderByOrderIndexAsc();
    long count();
}