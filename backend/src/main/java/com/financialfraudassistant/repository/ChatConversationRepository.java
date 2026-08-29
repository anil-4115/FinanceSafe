package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Integer> {
    List<ChatConversation> findByUserIdOrderByCreatedAtDesc(Integer userId);
    Optional<ChatConversation> findFirstByUserIdOrderByUpdatedAtDesc(Integer userId);
}