package com.dmvlab.swapserver;

import com.dmvlab.swapserver.commands.SwsCommand;
import com.dmvlab.swapserver.i18n.I18nManager;
import com.dmvlab.swapserver.manager.ServerListManager;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SwapServerPlugin extends JavaPlugin {
    /**
     * Creates the plugin instance with the provided init context.
     *
     * @param init plugin initialization data from the server
     */
    public SwapServerPlugin(JavaPluginInit init) {
        super(init);
    }

    /**
     * Initializes managers, registers commands, and logs startup.
     */
    @Override
    protected void start() {
        Path dataDirectory = Paths.get("mods/Swap_Server/data");
        ServerListManager serverManager = new ServerListManager(dataDirectory);

        getCommandRegistry().registerCommand(new SwsCommand(serverManager));

        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef == null) {
                return;
            }
            I18nManager i18nManager = new I18nManager(playerRef.getLanguage());
            i18nManager.sendTo(playerRef);
        });

        System.out.println("Swap_Server enabled!");
    }
}
