package com.springmud.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PlayerRepository extends MongoRepository<Player, String> {
    @Query("{id: '?0'}")
    Player findPlayerById(String id);
}
