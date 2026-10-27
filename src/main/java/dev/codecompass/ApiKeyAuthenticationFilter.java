package dev.codecompass;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String API_KEY_AUTH_ATTR = "API_KEY_AUTH";
    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawKey = resolveApiKey(request);
        if (rawKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<AppUser> user = apiKeyService.authenticateKey(rawKey);
            if (user.isPresent()) {
                UserPrincipal principal = new UserPrincipal(user.get());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(API_KEY_AUTH_ATTR, true);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveApiKey(HttpServletRequest request) {
        String header = request.getHeader("X-API-Key");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer cc_live_", 0, 15)) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
