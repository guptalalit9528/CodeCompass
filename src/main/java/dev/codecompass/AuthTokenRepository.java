package dev.codecompass;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByTokenHash(String tokenHash);
    long deleteAllByUser_IdAndPurpose(String userId, String purpose);
    void deleteAllByUser_Id(String userId);
}
