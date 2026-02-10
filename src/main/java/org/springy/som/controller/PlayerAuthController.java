package org.springy.som.controller;

import org.springy.som.client.SomApiClient;
import org.springy.som.client.model.PlayerView;
import org.springy.som.util.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;

@RestController
@RequestMapping(path = "/api/auth", produces = "application/json")
public class PlayerAuthController {
    private final SomApiClient somApiClient;
    private final PrivateKey privateKey;

    public PlayerAuthController(SomApiClient somApiClient, PrivateKey privateKey) {
        this.somApiClient = somApiClient;
        this.privateKey = privateKey;
    }

    @PostMapping(path = "/login", consumes = "application/json")
    public ResponseEntity<PlayerView> authenticate(@RequestBody PlayerAuthRequest request) {
        if (request == null || isBlank(request.accountName()) || isBlank(request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PlayerView player = somApiClient.getAccountByName(request.accountName());
        if (player == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String decrypted = CryptoUtil.decryptIfEncrypted(player.password(), privateKey);
        if (!request.password().equals(decrypted)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(player);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record PlayerAuthRequest(String accountName, String password) {}
}
