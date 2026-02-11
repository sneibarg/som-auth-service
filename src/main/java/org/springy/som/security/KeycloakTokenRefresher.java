package org.springy.som.security;

import org.springframework.scheduling.annotation.Scheduled;

public class KeycloakTokenRefresher {
    private final KeycloakTokenClient tokenClient;

    public KeycloakTokenRefresher(KeycloakTokenClient tokenClient) {
        this.tokenClient = tokenClient;
    }

    @Scheduled(fixedDelayString = "${som.keycloak.refresh.poll-ms:5000}", initialDelayString = "${som.keycloak.refresh.poll-ms:5000}")
    public void keepWarm() {
        tokenClient.refreshIfNeeded();
    }
}
