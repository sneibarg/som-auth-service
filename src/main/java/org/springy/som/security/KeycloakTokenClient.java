package org.springy.som.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springy.som.util.CryptoUtil;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class KeycloakTokenClient {
    private final RestClient restClient;
    private final SomProps props;
    private final PrivateKey privateKey;
    private final KeycloakRefreshProps refreshProps;
    private final String issuerUri;
    private final AtomicReference<URI> tokenEndpoint = new AtomicReference<>();

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    public KeycloakTokenClient(RestClient.Builder builder,
                               SomProps props,
                               PrivateKey privateKey,
                               KeycloakRefreshProps refreshProps,
                               @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        this.props = props;
        this.privateKey = privateKey;
        this.refreshProps = refreshProps;
        this.issuerUri = issuerUri;
        this.restClient = builder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        ensureFreshTokenOrThrow();
    }

    public String getAccessToken() {
        CachedToken existing = cache.get();
        if (existing != null && existing.isValid()) return existing.token();

        ensureFreshTokenOrThrow();
        CachedToken now = cache.get();
        if (now == null || !now.isValid()) {
            throw new IllegalStateException("No valid Keycloak token available");
        }
        return now.token();
    }

    public void ensureFreshTokenOrThrow() {
        if (!refreshLock.tryLock()) {
            waitForOtherRefresher();
            CachedToken now = cache.get();
            if (now == null || !now.isValid()) {
                throw new IllegalStateException("No valid Keycloak token available");
            }
            return;
        }
        int attempts = maxAttempts();
        RuntimeException lastError = null;
        try {
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    CachedToken current = cache.get();
                    if (current != null && current.isValid()) return;

                    TokenResponse tr = requestPasswordGrantToken();
                    cache.set(new CachedToken(tr.accessToken(), expiryInstant(tr.expiresIn())));
                    return;
                } catch (RuntimeException e) {
                    lastError = e;
                    sleep(backoffMs() * attempt);
                }
            }
        } finally {
            refreshLock.unlock();
        }
        throw new IllegalStateException("Unable to obtain Keycloak token after "
                + attempts + " attempts", lastError);
    }

    public boolean needsRefreshSoon() {
        CachedToken t = cache.get();
        if (t == null) return true;
        return Instant.now().isAfter(t.expiresAt().minusSeconds(skewSeconds()));
    }

    public void refreshIfNeeded() {
        if (!needsRefreshSoon()) return;
        if (!refreshLock.tryLock()) return;
        try {
            if (!needsRefreshSoon()) return;
            TokenResponse tr = requestPasswordGrantToken();
            cache.set(new CachedToken(tr.accessToken(), expiryInstant(tr.expiresIn())));
        } catch (RuntimeException e) {
            log.warn("Keycloak token refresh failed", e);
        } finally {
            refreshLock.unlock();
        }
    }

    private Instant expiryInstant(long expiresInSeconds) {
        long skew = skewSeconds();
        long effective = Math.max(1, expiresInSeconds - skew);
        return Instant.now().plusSeconds(effective);
    }

    private void waitForOtherRefresher() {
        long deadline = System.currentTimeMillis() + pollMs();
        while (System.currentTimeMillis() < deadline) {
            CachedToken t = cache.get();
            if (t != null && t.isValid()) return;
            sleep(25);
        }
    }

    private TokenResponse requestPasswordGrantToken() {
        SomProps.Keycloak kc = props.keycloak();
        log.info("Requesting password grant token with props={}", props);
        if (kc == null) {
            throw new IllegalStateException("som.keycloak configuration is required");
        }
        SomProps.InfraApi infraApi = props.infraApi();
        if (infraApi == null) {
            throw new IllegalStateException("som.infra-api configuration is required");
        }
        if ((kc.baseUrl() == null || kc.baseUrl().isBlank()) && (issuerUri == null || issuerUri.isBlank())) {
            throw new IllegalStateException("som.keycloak.base-url is required");
        }
        if ((kc.realm() == null || kc.realm().isBlank()) && (issuerUri == null || issuerUri.isBlank())) {
            throw new IllegalStateException("som.keycloak.realm is required");
        }

        URI tokenUri = resolveTokenEndpoint(kc);
        log.info("Keycloak token URL: {}", tokenUri);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        addIfPresent(form, "client_id", kc.clientId());
        addIfPresent(form, "client_secret", decryptIfEncrypted(kc.clientSecret()));
        addIfPresent(form, "username", decryptIfEncrypted(infraApi.username()));
        addIfPresent(form, "password", decryptIfEncrypted(infraApi.password()));
        log.info("issuerUri='{}'", issuerUri);
        log.info("baseUrl='{}' realm='{}'", kc.baseUrl(), kc.realm());
        log.info("Keycloak token URL: {}", tokenUri);

        TokenResponse response = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null) {
            throw new IllegalStateException("Keycloak token response was empty");
        }
        return response;
    }

    private String decryptIfEncrypted(String maybeEnc) {
        return CryptoUtil.decryptIfEncrypted(maybeEnc, privateKey);
    }

    private URI resolveTokenEndpoint(SomProps.Keycloak kc) {
        URI cached = tokenEndpoint.get();
        if (cached != null) return cached;

        URI resolved = discoverTokenEndpoint();
        if (resolved == null) {
            resolved = buildTokenUri(kc);
        }
        tokenEndpoint.compareAndSet(null, resolved);
        return tokenEndpoint.get();
    }

    private URI discoverTokenEndpoint() {
        if (issuerUri != null && !issuerUri.isBlank()) {
            String base = issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
            URI discoveryUri = UriComponentsBuilder.fromUriString(base)
                    .path("/.well-known/openid-configuration")
                    .build(true)
                    .toUri();
            try {
                KeycloakDiscovery discovery = restClient.get()
                        .uri(discoveryUri)
                        .retrieve()
                        .body(KeycloakDiscovery.class);
                if (discovery != null && discovery.tokenEndpoint() != null && !discovery.tokenEndpoint().isBlank()) {
                    return URI.create(discovery.tokenEndpoint());
                }
            } catch (RuntimeException e) {
                log.warn("Keycloak discovery failed; falling back to configured base URL", e);
            }
        }
        return null;
    }

    private URI buildTokenUri(SomProps.Keycloak kc) {
        String realm = normalizeRealm(kc.realm());
        return UriComponentsBuilder.fromHttpUrl(kc.baseUrl())
                .pathSegment("realms", realm, "protocol", "openid-connect", "token")
                .build()
                .encode()
                .toUri();
    }

    private static String normalizeRealm(String realm) {
        if (realm.contains("%")) {
            return URLDecoder.decode(realm, StandardCharsets.UTF_8);
        }
        return realm;
    }

    private static void addIfPresent(MultiValueMap<String, String> form, String key, String value) {
        if (value == null || value.isBlank()) return;
        form.add(key, value);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private int maxAttempts() {
        return refreshProps.maxAttempts() > 0 ? refreshProps.maxAttempts() : 1;
    }

    private long backoffMs() {
        return Math.max(0, refreshProps.backoffMs());
    }

    private long pollMs() {
        return refreshProps.pollMs() > 0 ? refreshProps.pollMs() : 5000;
    }

    private long skewSeconds() {
        return Math.max(0, refreshProps.skewSeconds());
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}

    public record KeycloakDiscovery(@JsonProperty("token_endpoint") String tokenEndpoint) {}

    public record CachedToken(String token, Instant expiresAt) {
        public boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
