package org.springy.som.client.model;

import java.util.List;
import java.util.Map;

public record CharacterView(
        String id,
        String accountId,
        String name,
        String title,
        String description,
        String sex,
        String roomId,
        String areaId,
        String guild,
        String role,
        Map<String, Object> equipped,
        Map<String, Object> promptFormat,
        Map<String, Object> characterRace,
        Map<String, Object> characterClass,
        Map<String, Object> characterFlags,
        Map<String, Object> armorClass,
        Map<String, Object> characterAttributes,
        Map<String, Object> statusFlags,
        List<Map<String, Object>> inventory,
        List<Map<String, Object>> effects,
        List<Map<String, Object>> skills,
        List<Map<String, Object>> spells,
        Boolean cloaked,
        int hit,
        int maxHit,
        int mana,
        int maxMana,
        int movement,
        int maxMovement,
        int level,
        int gold,
        int silver,
        int trust
) {}
