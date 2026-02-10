package org.springy.som.client;

import lombok.extern.slf4j.Slf4j;
import org.springy.som.client.model.CharacterView;
import org.springy.som.client.model.PlayerView;
import org.springy.som.security.KeycloakTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class SomApiClient {
    private static final ParameterizedTypeReference<List<RemotePlayerView>> PLAYER_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CharacterView>> CHARACTER_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KeycloakTokenClient tokenClient;
    private final String baseUrl;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private final AtomicReference<CacheSnapshot> cache = new AtomicReference<>();

    public SomApiClient(RestClient.Builder builder,
                        KeycloakTokenClient tokenClient,
                        @Value("${som.modulith.base-url}") String baseUrl) {
        this.restClient = builder.build();
        this.tokenClient = tokenClient;
        this.baseUrl = baseUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        refreshCache();
        this.getPlayerAccounts().forEach(account -> {
            log.info("Warmed up account: {}", account.accountName());
            account.playerCharacterList().forEach(character -> {
                log.info("Warmed up character: {}", character.name());
            });
        });
    }

    @Scheduled(fixedDelayString = "${som.keycloak.refresh.poll-ms:5000}", initialDelayString = "${som.keycloak.refresh.poll-ms:5000}")
    public void refreshCache() {
        if (!refreshLock.tryLock()) return;
        try {
            List<RemotePlayerView> players = fetchPlayerAccounts();
            CacheSnapshot refreshed = buildSnapshot(players);
            cache.set(refreshed);
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh modulith player cache", ex);
        } finally {
            refreshLock.unlock();
        }
    }

    public List<PlayerView> getPlayerAccounts() {
        CacheSnapshot snapshot = cache.get();
        if (snapshot == null) {
            refreshCache();
            snapshot = cache.get();
        }
        return snapshot == null ? List.of() : snapshot.players();
    }

    public PlayerView getAccountByName(String accountName) {
        if (accountName == null || accountName.isBlank()) return null;
        CacheSnapshot snapshot = cache.get();
        if (snapshot == null) {
            refreshCache();
            snapshot = cache.get();
        }
        if (snapshot == null) return null;
        return snapshot.players().stream()
                .filter(player -> accountName.equals(player.accountName()))
                .findFirst()
                .orElse(null);
    }

    private List<RemotePlayerView> fetchPlayerAccounts() {
        String token = tokenClient.getAccessToken();
        List<RemotePlayerView> response = restClient.get()
                .uri(uri("/api/v1/players"))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PLAYER_LIST);
        return response == null ? Collections.emptyList() : response;
    }

    private CacheSnapshot buildSnapshot(List<RemotePlayerView> players) {
        if (players == null || players.isEmpty()) {
            return new CacheSnapshot(List.of());
        }

        List<PlayerView> enrichedPlayers = new ArrayList<>();
        for (RemotePlayerView player : players) {
            String accountId = player.id();
            if (accountId == null || accountId.isBlank()) {
                enrichedPlayers.add(enrichPlayer(player, List.of()));
                continue;
            }
            List<CharacterView> accountCharacters = fetchCharactersByAccountId(accountId);
            enrichedPlayers.add(enrichPlayer(player, accountCharacters));
        }
        return new CacheSnapshot(List.copyOf(enrichedPlayers));
    }

    private List<CharacterView> fetchCharactersByAccountId(String accountId) {
        String token = tokenClient.getAccessToken();
        List<CharacterView> response = restClient.get()
                .uri(uri("/api/v1/characters/account/{accountId}", accountId))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(CHARACTER_LIST);
        return response == null ? Collections.emptyList() : response;
    }

    private String uri(String path, Object... args) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .buildAndExpand(args)
                .toUriString();
    }

    private PlayerView enrichPlayer(RemotePlayerView player, List<CharacterView> characters) {
        return new PlayerView(
                player.id(),
                player.firstName(),
                player.lastName(),
                player.accountName(),
                player.emailAddress(),
                player.password(),
                characters == null ? List.of() : List.copyOf(characters)
        );
    }

    private record CacheSnapshot(List<PlayerView> players) {}

    private record RemotePlayerView(
            String id,
            String firstName,
            String lastName,
            String accountName,
            String emailAddress,
            String password,
            List<String> playerCharacterList
    ) {}
}
