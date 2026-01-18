package com.dmvlab.swapserver.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationManager {
    private static final Map<UUID, Runnable> pending = new HashMap<>();

    public static void request(UUID uuid, Runnable action) {
        pending.put(uuid, action);
    }

    public static boolean confirm(UUID uuid, boolean yes) {
        Runnable action = pending.remove(uuid);
        if (action != null && yes) {
            action.run();
            return true;
        }
        return action != null;
    }
}
