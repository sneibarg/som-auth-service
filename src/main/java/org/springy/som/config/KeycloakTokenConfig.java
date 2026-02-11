package org.springy.som.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springy.som.security.KeycloakTokenClient;
import org.springy.som.security.KeycloakTokenRefresher;

@Configuration
public class KeycloakTokenConfig {
    private final KeycloakTokenClient tokenClient;

    public KeycloakTokenConfig(KeycloakTokenClient tokenClient) {
        this.tokenClient = tokenClient;
    }

    @Bean
    public KeycloakTokenRefresher keycloakTokenRefresher() {
        return new KeycloakTokenRefresher(tokenClient);
    }
}
