package dev.codecompass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {
    private static final String KEY_PREFIX = "cc_live_";
    private static final int MAX_ACTIVE_KEYS_PER_USER = 10;

    private final ApiKeyRepository apiKeyRepository;
    private final AppUserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AppUserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SettingsDtos.ApiKeyDto> listKeys(String userId) {
        return apiKeyRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SettingsDtos.ApiKeyDto::from)
                .toList();
    }

    @Transactional
    public SettingsDtos.CreateApiKeyResponse createKey(String userId, SettingsDtos.CreateApiKeyRequest request) {
        long activeCount = apiKeyRepository.countByUser_IdAndRevokedAtIsNull(userId);
        if (activeCount >= MAX_ACTIVE_KEYS_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Active API key limit (" + MAX_ACTIVE_KEYS_PER_USER + ") reached. Please revoke an unused key.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));

        byte[] entropy = new byte[24];
        secureRandom.nextBytes(entropy);
        String randomHex = HexFormat.of().formatHex(entropy);
        String rawKey = KEY_PREFIX + randomHex;

        String visiblePrefix = KEY_PREFIX + randomHex.substring(0, 6) + "•••";
        String hash = sha256(rawKey);

        String scopes = request.scopes();
        if (scopes == null || scopes.isBlank()) {
            scopes = "chat:write,conversations:read";
        }

        ApiKey apiKey = new ApiKey(user, request.name().trim(), visiblePrefix, hash, scopes);
        apiKey = apiKeyRepository.saveAndFlush(apiKey);

        return new SettingsDtos.CreateApiKeyResponse(SettingsDtos.ApiKeyDto.from(apiKey), rawKey);
    }

    @Transactional
    public void revokeKey(String userId, String keyId) {
        ApiKey key = apiKeyRepository.findByIdAndUser_Id(keyId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API key not found."));
        key.revoke();
        apiKeyRepository.save(key);
    }

    @Transactional
    public Optional<AppUser> authenticateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }
        String hash = sha256(rawKey.trim());
        Optional<ApiKey> optionalKey = apiKeyRepository.findByKeyHash(hash);
        if (optionalKey.isEmpty()) {
            return Optional.empty();
        }

        ApiKey key = optionalKey.get();
        if (key.isRevoked() || !key.getUser().isEnabled()) {
            return Optional.empty();
        }

        key.touchLastUsed();
        apiKeyRepository.save(key);
        return Optional.of(key.getUser());
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
