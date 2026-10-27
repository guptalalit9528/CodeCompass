package dev.codecompass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findAllByUser_IdOrderByCreatedAtDesc(String userId);
    Optional<ApiKey> findByIdAndUser_Id(String id, String userId);
    void deleteAllByUser_Id(String userId);
    long countByUser_IdAndRevokedAtIsNull(String userId);
}
