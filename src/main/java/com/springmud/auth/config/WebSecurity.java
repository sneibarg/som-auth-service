package com.springmud.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurity {
    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf().disable()
            .authorizeRequests()
                .requestMatchers("/actuator/*")
            .permitAll()
            .and().build();
            /*.permitAll()
            .and()
            .httpBasic(withDefaults())
            .authorizeRequests()
            .anyRequest()
            .authenticated()
            .and()
            .build(); */
    }
}
