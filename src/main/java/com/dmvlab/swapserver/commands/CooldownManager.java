package com.dmvlab.swapserver.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private static final Map<UUID, Long> lastUse = new HashMap<>();

    public static boolean canUse(UUID uuid, int cooldownSeconds) {
        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(uuid, 0L);
        if (now - last < cooldownSeconds * 1000L)
            return false;
        lastUse.put(uuid, now);
        return true;
    }
}
