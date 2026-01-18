package com.dmvlab.swapserver.listeners;

import com.google.gson.Gson;
import com.dmvlab.swapserver.manager.ConfigManager;
import com.dmvlab.swapserver.manager.PositionManager;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.entity.entities.Player; // Updated import

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.dmvlab.swapserver.model.PlayerPosition;

public class PlayerEventListener {
    private final PositionManager positionManager;
    private final ConfigManager configManager;
    private final Gson gson = new Gson();

    public PlayerEventListener(PositionManager pm, ConfigManager cm) {
        this.positionManager = pm;
        this.configManager = cm;
    }

    public void onReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUuid().toString();
        PlayerPosition pos = positionManager.getPlayerPosition(uuid, configManager.getServerName());

        if (pos != null) {
            PlayerRef ref = player.getPlayerRef();

            try {

                ref.updatePosition(player.getWorld(), new Transform(new Vector3d(pos.x, pos.y, pos.z)),
                        new Vector3f(pos.pitch, pos.yaw, 0));
            } catch (Exception e) {
                System.err.println("Failed to restore position: " + e.getMessage());
            }
        }
    }
}
