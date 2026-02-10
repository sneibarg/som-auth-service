package org.springy.som.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springy.som.client.SomApiClient;
import org.springy.som.client.model.PlayerView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.PrivateKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerAuthControllerTest {

    @Mock
    private SomApiClient somApiClient;

    @Mock
    private PrivateKey privateKey;

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

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey);
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

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                "alice",
                "wrong"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticate_rejectsMissingAccount() {
        when(somApiClient.getAccountByName("alice")).thenReturn(null);

        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                "alice",
                "secret"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticate_rejectsBlankRequest() {
        PlayerAuthController controller = new PlayerAuthController(somApiClient, privateKey);
        ResponseEntity<PlayerView> response = controller.authenticate(new PlayerAuthController.PlayerAuthRequest(
                " ",
                ""
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
