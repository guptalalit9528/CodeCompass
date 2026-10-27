package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextItemRepository extends JpaRepository<ContextItem, String> {
    List<ContextItem> findAllByUser_IdAndConversationIdOrderByCreatedAtDesc(String userId, String conversationId);
    List<ContextItem> findAllByUser_IdAndPinnedTrue(String userId);
    Optional<ContextItem> findByIdAndUser_Id(String id, String userId);
    void deleteAllByUser_Id(String userId);
}
