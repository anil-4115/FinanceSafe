package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {
    List<QuizQuestion> findByModuleId(Integer moduleId);
}