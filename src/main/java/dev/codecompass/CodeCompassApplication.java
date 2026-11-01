package dev.codecompass;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.MapPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class CodeCompassApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CodeCompassApplication.class);
        application.addInitializers(context -> {
            loadDotenv(context.getEnvironment());
            normalizePostgresUrl(context.getEnvironment());
        });
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
            int schemeEnd = configuredUrl.indexOf("://");
            String afterScheme = configuredUrl.substring(schemeEnd + 3);

            String userInfo = null;
            String hostAndPath = afterScheme;
            int atIndex = afterScheme.lastIndexOf('@');
            if (atIndex != -1) {
                userInfo = afterScheme.substring(0, atIndex);
                hostAndPath = afterScheme.substring(atIndex + 1);
            }

            String pathAndQuery = "";
            String hostAndPort = hostAndPath;
            int slashIndex = hostAndPath.indexOf('/');
            int queryIndex = hostAndPath.indexOf('?');
            int pathStart = slashIndex != -1 ? slashIndex : queryIndex;
            if (pathStart != -1) {
                hostAndPort = hostAndPath.substring(0, pathStart);
                pathAndQuery = hostAndPath.substring(pathStart);
            }

            if (hostAndPort.isBlank()) {
                throw new IllegalArgumentException("Database URL must include a valid host.");
            }

            String jdbcUrl = "jdbc:postgresql://" + hostAndPort + pathAndQuery;
            if (!jdbcUrl.contains("sslmode=")) {
                jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("spring.datasource.url", jdbcUrl);

            if (userInfo != null && !userInfo.isBlank()) {
                String[] credentials = userInfo.split(":", 2);
                if (!credentials[0].isEmpty() && !hasDatabaseUsername(environment)) {
                    normalized.put("spring.datasource.username", decodeUriComponent(credentials[0]));
                }
                if (credentials.length == 2 && !hasDatabasePassword(environment)) {
                    String pwd = credentials[1];
                    if (!"[YOUR-PASSWORD]".equalsIgnoreCase(pwd) && !"your-password".equalsIgnoreCase(pwd)) {
                        normalized.put("spring.datasource.password", decodeUriComponent(pwd));
                    }
                }
            }
            environment.getPropertySources().addFirst(new MapPropertySource("normalizedPostgresUrl", normalized));
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid PostgreSQL database URL. Use a postgres:// or postgresql:// URL with a valid host, "
                            + "or provide a JDBC URL beginning jdbc:postgresql://.", exception);
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

    private static void loadDotenv(ConfigurableEnvironment environment) {
        List<Path> candidateFiles = List.of(
                Path.of(".env.local"),
                Path.of(".env")
        );
        Map<String, Object> envProperties = new LinkedHashMap<>();
        for (Path path : candidateFiles) {
            if (Files.isRegularFile(path)) {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                        int equalsIdx = trimmed.indexOf('=');
                        if (equalsIdx > 0) {
                            String key = trimmed.substring(0, equalsIdx).trim();
                            String value = trimmed.substring(equalsIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
                                    || (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (!key.isEmpty()) {
                                envProperties.putIfAbsent(key, value);
                            }
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        if (!envProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envProperties));
        }
    }
}
