package dev.codecompass;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEvent, String> {
    List<UsageEvent> findTop50ByUser_IdOrderByCreatedAtDesc(String userId);
    List<UsageEvent> findAllByUser_IdAndCreatedAtAfter(String userId, LocalDateTime since);
    long countByUser_Id(String userId);
    long countByUser_IdAndStatus(String userId, String status);
    long countByUser_IdAndSource(String userId, String source);
    void deleteAllByUser_Id(String userId);
}
