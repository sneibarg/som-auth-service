package org.springy.som.controller;

import org.springy.som.client.SomApiClient;
import org.springy.som.client.model.PlayerView;
import org.springy.som.util.CryptoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/auth", produces = "application/json")
public class PlayerAuthController {
    private final SomApiClient somApiClient;
    private final PrivateKey privateKey;
    private final JwtDecoder jwtDecoder;

    public PlayerAuthController(SomApiClient somApiClient, PrivateKey privateKey, JwtDecoder jwtDecoder) {
        this.somApiClient = somApiClient;
        this.privateKey = privateKey;
        this.jwtDecoder = jwtDecoder;
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

    @GetMapping(path = "/token/decode")
    public ResponseEntity<Map<String, Object>> decodeToken(@RequestHeader(name = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return ResponseEntity.ok(jwt.getClaims());
        } catch (JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) return null;
        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) return null;
        String token = authorization.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    public record PlayerAuthRequest(String accountName, String password) {}
}
