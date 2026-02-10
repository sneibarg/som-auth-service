package org.springy.som.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "som")
public record SomProps(WebSecurity webSecurity, Keycloak keycloak, InfraApi infraApi) {
    public record WebSecurity(Api api) {
        public record Api(String username, String password) {}
    }
    public record Keycloak(String baseUrl, String realm, String clientId, String clientSecret) {}
    public record InfraApi(String username, String password) {}
}
