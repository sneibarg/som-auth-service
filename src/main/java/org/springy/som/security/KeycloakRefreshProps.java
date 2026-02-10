package org.springy.som.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "som.keycloak.refresh")
public record KeycloakRefreshProps(
        long skewSeconds,
        long pollMs,
        int maxAttempts,
        long backoffMs
) {}
