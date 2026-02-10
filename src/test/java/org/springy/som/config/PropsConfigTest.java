package org.springy.som.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springy.som.security.KeycloakRefreshProps;
import org.springy.som.security.SomProps;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PropsConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropsConfig.class, ConfigurationPropertiesAutoConfiguration.class);

    @Test
    void bindsConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "som.keycloak.base-url=http://example",
                        "som.keycloak.realm=realm",
                        "som.keycloak.client-id=client",
                        "som.keycloak.client-secret=secret",
                        "som.infra-api.username=infra",
                        "som.infra-api.password=infra-pass",
                        "som.keycloak.refresh.skew-seconds=5",
                        "som.keycloak.refresh.poll-ms=1500",
                        "som.keycloak.refresh.max-attempts=2",
                        "som.keycloak.refresh.backoff-ms=100"
                )
                .run(context -> {
                    SomProps props = context.getBean(SomProps.class);
                    assertThat(props.keycloak().baseUrl()).isEqualTo("http://example");
                    assertThat(props.keycloak().realm()).isEqualTo("realm");
                    assertThat(props.keycloak().clientId()).isEqualTo("client");
                    assertThat(props.keycloak().clientSecret()).isEqualTo("secret");
                    assertThat(props.infraApi().username()).isEqualTo("infra");
                    assertThat(props.infraApi().password()).isEqualTo("infra-pass");

                    KeycloakRefreshProps refresh = context.getBean(KeycloakRefreshProps.class);
                    assertThat(refresh.skewSeconds()).isEqualTo(5);
                    assertThat(refresh.pollMs()).isEqualTo(1500);
                    assertThat(refresh.maxAttempts()).isEqualTo(2);
                    assertThat(refresh.backoffMs()).isEqualTo(100);
                });
    }
}
