package org.springy.som.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;

@Configuration
public class KeyStoreConfig {
    @Bean
    public PrivateKey configPrivateKey(@Value("${som.crypto.keystore.location}") String location,
                                       @Value("${som.crypto.keystore.password}") String storePassword,
                                       @Value("${som.crypto.key.alias}") String alias,
                                       @Value("${som.crypto.key.password}") String keyPassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = openLocation(location)) {
            ks.load(is, storePassword.toCharArray());
        }

        Key key = ks.getKey(alias, keyPassword.toCharArray());
        if (!(key instanceof PrivateKey pk)) {
            throw new IllegalStateException("Alias '" + alias + "' did not resolve to a PrivateKey");
        }

        return pk;
    }

    private InputStream openLocation(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            String cp = location.substring("classpath:".length());
            InputStream is = getClass().getResourceAsStream(cp.startsWith("/") ? cp : "/" + cp);
            if (is == null) throw new IllegalArgumentException("Classpath resource not found: " + location);
            return is;
        }
        return new java.io.FileInputStream(location);
    }
}
