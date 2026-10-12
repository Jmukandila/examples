package io.miabi.examples;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@code postgres://user:password@host:port/db} connection string, as Miabi injects it, split
 * into the JDBC URL and credentials Spring's datasource expects.
 */
public record DatabaseUrl(String jdbcUrl, String username, String password) {

    public static Optional<DatabaseUrl> fromEnv(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parse(value));
    }

    public static DatabaseUrl parse(String value) {
        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException("DB_URL must be a postgres:// URL, got " + scheme + "://");
        }
        String username = null;
        String password = null;
        if (uri.getRawUserInfo() != null) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            username = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : null;
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath()
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        return new DatabaseUrl(jdbcUrl, username, password);
    }

    public Map<String, Object> datasourceProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        if (username != null) {
            props.put("spring.datasource.username", username);
        }
        if (password != null) {
            props.put("spring.datasource.password", password);
        }
        return props;
    }

    // URLDecoder is a form decoder: escape '+' first so a literal plus in a password survives.
    private static String decode(String s) {
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
