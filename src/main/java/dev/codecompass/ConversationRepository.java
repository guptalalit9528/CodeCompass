package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByIdAndOwner_Id(String id, String ownerId);
    List<Conversation> findAllByOwner_IdOrderByUpdatedAtDesc(String ownerId);
    List<Conversation> findAllByOwner_IdAndProject_IdOrderByUpdatedAtDesc(String ownerId, String projectId);
    List<Conversation> findAllByOwner_IdAndProjectIsNullOrderByUpdatedAtDesc(String ownerId);
    List<Conversation> findAllByOwner_IdAndProject_IdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String ownerId, String projectId, String title);
    List<Conversation> findAllByOwner_IdAndProjectIsNullAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String ownerId, String title);
    void deleteAllByOwner_Id(String ownerId);
}
