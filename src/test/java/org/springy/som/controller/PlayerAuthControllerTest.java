package org.springy.som.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springy.som.client.SomApiClient;
import org.springy.som.client.model.PlayerView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerAuthControllerTest {

    @Mock
    private SomApiClient somApiClient;

    @Mock
    private PrivateKey privateKey;

    @Mock
    private JwtDecoder jwtDecoder;

    @Test
    void authenticate_returnsNoContentOnValidCredentials() {
        PlayerView player = new PlayerView(
                "acct-1",
                "Alice",
                "Wonder",
                "alice",
                "alice@example.com",
                "secret",
                List.of()
        );
        when(somApiClient.getAccountByName("alice")).thenReturn(player);

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                "alice",
                "secret"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(player);
    }

    @Test
    void authenticate_rejectsWrongPassword() {
        PlayerView player = new PlayerView(
                "acct-1",
                "Alice",
                "Wonder",
                "alice",
                "alice@example.com",
                "secret",
                List.of()
        );
        when(somApiClient.getAccountByName("alice")).thenReturn(player);

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                "alice",
                "wrong"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticate_rejectsMissingAccount() {
        when(somApiClient.getAccountByName("alice")).thenReturn(null);

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                "alice",
                "secret"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticate_rejectsBlankRequest() {
        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                " ",
                ""
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void decodeToken_returnsClaimsOnValidToken() {
        when(jwtDecoder.decode("good")).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("good")
                .header("alg", "none")
                .claim("sub", "alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build());

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<java.util.Map<String, Object>> response = controller.decodeToken("Bearer good");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("sub", "alice");
    }

    @Test
    void decodeToken_rejectsMissingBearer() {
        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<java.util.Map<String, Object>> response = controller.decodeToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void decodeToken_rejectsInvalidToken() {
        when(jwtDecoder.decode("bad")).thenThrow(new JwtException("invalid"));

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey, jwtDecoder);
        ResponseEntity<java.util.Map<String, Object>> response = controller.decodeToken("Bearer bad");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
