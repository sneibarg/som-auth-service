package org.springy.som.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = WebSecurityConfigTest.TestApp.class)
@AutoConfigureMockMvc
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpointIsPermitted() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountName\":\"a\",\"password\":\"b\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDecodeEndpointIsPermitted() throws Exception {
        mockMvc.perform(get("/api/auth/token/decode"))
                .andExpect(status().isOk());
    }

    @Test
    void otherEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/auth/other"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({WebSecurityConfig.class, JwtAuthConverterConfig.class})
    static class TestApp {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();
        }

        @RestController
        static class TestController {
            @PostMapping(path = "/api/auth/login", consumes = "application/json")
            String login(@RequestBody String body) {
                return body;
            }

            @GetMapping("/api/auth/other")
            String other() {
                return "ok";
            }

            @GetMapping("/api/auth/token/decode")
            String decode() {
                return "ok";
            }
        }
    }
}
