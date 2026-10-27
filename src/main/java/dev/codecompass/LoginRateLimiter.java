package dev.codecompass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Per-process brute-force protection keyed by remote address. */
@Component
public class LoginRateLimiter {
    private final ConcurrentHashMap<String, Bucket> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public LoginRateLimiter(@Value("${codecompass.auth.login.max-attempts:5}") int maxAttempts,
                            @Value("${codecompass.auth.login.window-seconds:900}") long windowSeconds) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.windowMillis = Math.max(60, windowSeconds) * 1000;
    }

    public void check(String key) {
        Bucket bucket = attempts.get(key);
        if (bucket != null && System.currentTimeMillis() < bucket.expiresAt && bucket.failures.get() >= maxAttempts) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many sign-in attempts. Wait a while before trying again.");
        }
        if (attempts.size() > 10000) attempts.entrySet().removeIf(e -> System.currentTimeMillis() >= e.getValue().expiresAt);
    }

    public void failed(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (ignored, current) -> {
            Bucket bucket = current == null || now >= current.expiresAt ? new Bucket(now + windowMillis) : current;
            bucket.failures.incrementAndGet();
            return bucket;
        });
    }

    public void succeeded(String key) { attempts.remove(key); }

    private static final class Bucket {
        private final long expiresAt;
        private final AtomicInteger failures = new AtomicInteger();
        private Bucket(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
