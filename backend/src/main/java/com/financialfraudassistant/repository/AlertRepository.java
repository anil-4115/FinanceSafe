package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AlertRepository extends JpaRepository<Alert, Integer> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(Integer userId);
    Optional<Alert> findByIdAndUserId(Integer id, Integer userId);
}
