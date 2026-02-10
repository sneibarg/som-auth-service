package org.springy.som.client.model;

import java.util.List;

public record PlayerView(
        String id,
        String firstName,
        String lastName,
        String accountName,
        String emailAddress,
        String password,
        List<CharacterView> playerCharacterList
) {}
