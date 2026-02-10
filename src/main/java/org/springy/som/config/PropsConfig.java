package org.springy.som.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springy.som.security.KeycloakRefreshProps;
import org.springy.som.security.SomProps;

@EnableConfigurationProperties({SomProps.class, KeycloakRefreshProps.class})
@Configuration
class PropsConfig {}
