package com.financialfraudassistant.repository;
import com.financialfraudassistant.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Integer conversationId);
    List<ChatMessage> findByConversationIdOrderById(Integer conversationId);
}