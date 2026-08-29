package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.EducationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EducationAttemptRepository extends JpaRepository<EducationAttempt, Integer> {
    List<EducationAttempt> findByUserId(Integer userId);
}