package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByOwner_IdOrderByUpdatedAtDesc(String ownerId);
    Optional<Project> findByIdAndOwner_Id(String id, String ownerId);
    void deleteAllByOwner_Id(String ownerId);
}
