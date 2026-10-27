package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedCollaboratorRepository extends JpaRepository<SharedCollaborator, String> {
    List<SharedCollaborator> findAllByConversation_IdOrderByCreatedAtAsc(String conversationId);
    boolean existsByConversation_IdAndCollaboratorEmailIgnoreCase(String conversationId, String email);
    Optional<SharedCollaborator> findByIdAndConversation_Owner_Id(String id, String ownerId);
    void deleteAllByUser_Id(String userId);
    void deleteAllByConversation_Id(String conversationId);
}
