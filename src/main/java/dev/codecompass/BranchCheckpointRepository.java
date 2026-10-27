package dev.codecompass;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchCheckpointRepository extends JpaRepository<BranchCheckpoint, String> {
    List<BranchCheckpoint> findAllByConversation_IdOrderByCreatedAtDesc(String conversationId);
    void deleteAllByConversation_Id(String conversationId);
}
