package org.springy.som.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeycloakTokenRefresher {
    private final KeycloakTokenClient tokenClient;

    public KeycloakTokenRefresher(KeycloakTokenClient tokenClient) {
        this.tokenClient = tokenClient;
    }

    @Scheduled(
            fixedDelayString = "${som.keycloak.refresh.poll-ms:5000}",
            initialDelayString = "${som.keycloak.refresh.poll-ms:5000}"
    )
    public void keepWarm() {
        tokenClient.refreshIfNeeded();
    }
}
