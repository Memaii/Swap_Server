package com.dmvlab.swapserver.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class SwapServerPage extends BasicCustomUIPage {
    public static final String UI_PATH = "Pages/swap_server_hud.ui";

    public SwapServerPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
    }

    @Override
    public void build(UICommandBuilder builder) {
        builder.append(UI_PATH);
    }
}
