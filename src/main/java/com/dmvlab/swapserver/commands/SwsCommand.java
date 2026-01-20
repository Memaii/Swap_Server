package com.dmvlab.swapserver.commands;

import com.dmvlab.swapserver.manager.ServerListManager;
import com.dmvlab.swapserver.ui.SwapServerPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.dmvlab.swapserver.i18n.I18nManager;

public class SwsCommand extends CommandBase {
    private final ServerListManager serverManager;

    /**
     * Creates the command handler with its dependencies.
     *
     * @param serverManager manager for the server list
     */
    public SwsCommand(ServerListManager serverManager) {
        super("sws", "swapserver.commands.sws.desc");
        this.serverManager = serverManager;
    }

    /**
     * Executes the command and opens the UI for players only.
     *
     * @param context command context provided by the server
     */
    @Override
    protected void executeSync(CommandContext context) {
        if (!context.isPlayer()) {
            context.sender().sendMessage(Message.raw("Players only."));
            return;
        }
        openSwapServerUi(context);
    }

    /**
     * Resolves the player entity and opens the SwapServer UI on the world thread.
     *
     * @param context command context provided by the server
     */
    private void openSwapServerUi(CommandContext context) {
        Ref<EntityStore> entityRef = context.senderAsPlayerRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        EntityStore entityStore = store.getExternalData();
        if (entityStore == null) {
            return;
        }

        World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> {
            if (!entityRef.isValid()) {
                return;
            }

            Player player = store.getComponent(entityRef, Player.getComponentType());
            if (player == null) {
                return;
            }

            if (player.getPageManager().getCustomPage() != null) {
                return;
            }

            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            I18nManager i18nManager = new I18nManager(playerRef.getLanguage());
            i18nManager.sendTo(playerRef);

            player.getPageManager().openCustomPage(entityRef, store,
                    new SwapServerPage(playerRef, serverManager, i18nManager));
        });
    }
}
