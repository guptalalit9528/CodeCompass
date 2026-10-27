package dev.codecompass;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findAllByConversation_Id(String conversationId, Sort sort);
    List<ChatMessage> findAllByConversation_Id(String conversationId, Pageable pageable);
    long deleteAllByConversation_Id(String conversationId);
}
