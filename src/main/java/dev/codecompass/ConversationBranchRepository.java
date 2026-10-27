package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationBranchRepository extends JpaRepository<ConversationBranch, String> {
    List<ConversationBranch> findAllByConversation_IdOrderByCreatedAtAsc(String conversationId);
    Optional<ConversationBranch> findByIdAndConversation_Owner_Id(String id, String ownerId);
    void deleteAllByConversation_Id(String conversationId);
}
