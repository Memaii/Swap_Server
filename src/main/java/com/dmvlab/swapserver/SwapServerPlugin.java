package com.dmvlab.swapserver;

import com.dmvlab.swapserver.manager.ConfigManager;
import com.dmvlab.swapserver.manager.PositionManager;
import com.dmvlab.swapserver.manager.ServerManager;
import com.dmvlab.swapserver.commands.SwsCommand;
import com.dmvlab.swapserver.listeners.PlayerEventListener;
import com.dmvlab.swapserver.i18n.I18nManager;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SwapServerPlugin extends JavaPlugin {
    private ServerManager serverManager;
    private PositionManager positionManager;
    private ConfigManager configManager;
    private I18nManager i18nManager;

    public SwapServerPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        // Initialize managers
        Path dataFolder = Paths.get("mods/Swap_Server/data");
        configManager = new ConfigManager(dataFolder);
        serverManager = new ServerManager(dataFolder);
        positionManager = new PositionManager(dataFolder);
        i18nManager = new I18nManager("fr_FR");

        // Register commands
        HytaleServer.get().getCommandManager()
                .register(new SwsCommand(serverManager, positionManager, configManager, i18nManager));

        // Register listeners (Functional API)
        PlayerEventListener listener = new PlayerEventListener(positionManager, configManager);
        HytaleServer.get().getEventBus().registerGlobal(PlayerReadyEvent.class, listener::onReady);

        System.out.println("Swap_Server enabled!");
    }
}
