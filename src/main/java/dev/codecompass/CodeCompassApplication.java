package dev.codecompass;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.MapPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class CodeCompassApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CodeCompassApplication.class);
        application.addInitializers(context -> normalizePostgresUrl(context.getEnvironment()));
        application.run(args);
    }

    private static void normalizePostgresUrl(ConfigurableEnvironment environment) {
        String configuredUrl = environment.getProperty("SUPABASE_DB_URL");
        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = environment.getProperty("DB_URL");
        }
        if (configuredUrl == null) return;

        String lowerUrl = configuredUrl.toLowerCase();
        if (!lowerUrl.startsWith("postgres://") && !lowerUrl.startsWith("postgresql://")) return;

        try {
            URI uri = URI.create(configuredUrl);
            String authority = uri.getRawAuthority();
            if (uri.getHost() == null || authority == null) {
                throw new IllegalArgumentException("Database URL must include a valid host.");
            }

            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                authority = authority.substring(authority.lastIndexOf('@') + 1);
            }
            String jdbcUrl = "jdbc:postgresql://" + authority
                    + (uri.getRawPath() == null ? "" : uri.getRawPath())
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("spring.datasource.url", jdbcUrl);
            if (userInfo != null) {
                String[] credentials = userInfo.split(":", 2);
                if (!credentials[0].isEmpty() && !hasDatabaseUsername(environment)) {
                    normalized.put("spring.datasource.username", decodeUriComponent(credentials[0]));
                }
                if (credentials.length == 2 && !hasDatabasePassword(environment)) {
                    normalized.put("spring.datasource.password", decodeUriComponent(credentials[1]));
                }
            }
            environment.getPropertySources().addFirst(new MapPropertySource("normalizedPostgresUrl", normalized));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid PostgreSQL database URL. Use a postgres:// or postgresql:// URL with a valid host, "
                            + "or provide a JDBC URL beginning jdbc:postgresql://.");
        }
    }

    private static boolean hasDatabaseUsername(ConfigurableEnvironment environment) {
        return hasText(environment.getProperty("SUPABASE_DB_USERNAME"))
                || hasText(environment.getProperty("DB_USERNAME"));
    }

    private static boolean hasDatabasePassword(ConfigurableEnvironment environment) {
        return hasText(environment.getProperty("SUPABASE_DB_PASSWORD"))
                || hasText(environment.getProperty("DB_PASSWORD"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String decodeUriComponent(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
