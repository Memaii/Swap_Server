package com.dmvlab.swapserver.ui;

import com.dmvlab.swapserver.i18n.I18nManager;
import com.dmvlab.swapserver.manager.ServerListManager;
import com.dmvlab.swapserver.model.ServerEntry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Optional;

public class SwapServerPage extends InteractiveCustomUIPage<SwapServerPage.SwapServerPageEventData> {
    private static final String HUD_UI_PATH = "Pages/swap_server_hud.ui";
    private static final String ADMIN_HUD_UI_PATH = "Pages/swap_server_hud_admin.ui";
    private static final String ENTRY_UI_PATH = "Pages/entry.ui";
    private static final String ADMIN_ENTRY_UI_PATH = "Pages/entry_admin.ui";
    private static final String ADD_SERVER_UI_PATH = "Pages/add_server.ui";
    private static final int DEFAULT_SERVER_PORT = 5520;

    private final ServerListManager serverManager;
    private final I18nManager i18nManager;

    /**
     * Creates the swap server UI page for the given player.
     *
     * @param playerRef the player reference used to target updates
     * @param serverManager manager for the server list
     * @param i18nManager manager for localized messages
     */
    public SwapServerPage(PlayerRef playerRef, ServerListManager serverManager, I18nManager i18nManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SwapServerPageEventData.CODEC);
        this.serverManager = serverManager;
        this.i18nManager = i18nManager;
    }

    /**
     * Builds the UI layout and binds events for the current player.
     *
     * @param ref reference to the entity store
     * @param commandBuilder builder used to compose UI commands
     * @param eventBuilder builder used to bind UI events
     * @param store entity component store
     */
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder,
            Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        boolean isAdmin = player != null && player.hasPermission("swapserver.admin");
        commandBuilder.append(isAdmin ? ADMIN_HUD_UI_PATH : HUD_UI_PATH);
        commandBuilder.clear("#AddServer");
        commandBuilder.set("#Title.Text", i18nManager.translate("ui_title"));

        if (isAdmin) {
            commandBuilder.append("#AddServer", ADD_SERVER_UI_PATH);
            commandBuilder.set("#NewServerName.PlaceholderText", i18nManager.translate("ui_placeholder_name"));
            commandBuilder.set("#NewServerIp.PlaceholderText", i18nManager.translate("ui_placeholder_ip"));
            commandBuilder.set("#AddButton.Text", i18nManager.translate("ui_add"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton",
                    new EventData().append("@NewServerName", "#NewServerName.Value").append("@NewServerIp",
                            "#NewServerIp.Value"),
                    false);
        }

        populateServerList(commandBuilder, eventBuilder, isAdmin);
    }

    /**
     * Routes incoming UI event data to the correct handler.
     *
     * @param ref reference to the entity store
     * @param store entity component store
     * @param eventData decoded event payload
     */
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SwapServerPageEventData eventData) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (eventData.serverNameToDelete != null) {
            handleDeleteServerRequest(player, ref, store, eventData.serverNameToDelete);
            return;
        }

        if (eventData.joinServerName != null) {
            handleJoinRequest(player, ref, store, eventData.joinServerName);
            return;
        }

        if (eventData.newServerNameInput != null || eventData.newServerAddressInput != null) {
            handleAddServerRequest(player, ref, store, eventData.newServerNameInput,
                    eventData.newServerAddressInput);
        }
    }

    /**
     * Rebuilds the server list UI and attaches join/delete bindings.
     *
     * @param commandBuilder builder used to compose UI commands
     * @param eventBuilder builder used to bind UI events
     * @param isAdmin whether admin-only controls should be added
     */
    private void populateServerList(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, boolean isAdmin) {
        commandBuilder.clear("#ServerList");
        List<ServerEntry> serverEntries = serverManager.getServerList();
        String entryTemplatePath = isAdmin ? ADMIN_ENTRY_UI_PATH : ENTRY_UI_PATH;
        String joinLabel = i18nManager.translate("ui_join");
        String noServersLabel = i18nManager.translate("ui_no_servers");

        int index = 0;
        for (ServerEntry entry : serverEntries) {
            if (entry == null || entry.getName() == null) {
                continue;
            }

            String entrySelector = "#ServerList[" + index + "]";
            commandBuilder.append("#ServerList", entryTemplatePath);
            commandBuilder.set(entrySelector + " #ServerName.Text", entry.getName());
            commandBuilder.set(entrySelector + " #JoinButton.Text", joinLabel);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, entrySelector + " #JoinButton",
                    EventData.of("ServerName", entry.getName()), false);

            if (isAdmin) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, entrySelector + " #DeleteButton",
                        EventData.of("DeleteServerName", entry.getName()), false);
            }

            index++;
        }

        if (index == 0) {
            commandBuilder.appendInline("#ServerList",
                    "Label { Text: \"" + noServersLabel + "\"; Style: (Alignment: Center); }");
        }
    }

    /**
     * Validates the target name and transfers the player if a match is found.
     *
     * @param player player requesting the transfer
     * @param ref reference to the entity store
     * @param store entity component store
     * @param targetServerName server name selected in the UI
     */
    private void handleJoinRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
            String targetServerName) {
        if (targetServerName == null) {
            return;
        }

        Optional<ServerEntry> targetServer = serverManager.findServerByName(targetServerName);
        if (!targetServer.isPresent()) {
            player.sendMessage(Message.raw(i18nManager.translate("server_not_found", targetServerName)));
            return;
        }

        transferToServer(player, targetServer.get(), ref, store);
    }

    /**
     * Validates admin input, adds the server, and refreshes the list UI.
     *
     * @param player player submitting the new server
     * @param ref reference to the entity store
     * @param store entity component store
     * @param newServerName name input from the UI
     * @param newServerAddress address input from the UI (host or host:port)
     */
    private void handleAddServerRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
            String newServerName, String newServerAddress) {
        if (!player.hasPermission("swapserver.admin")) {
            player.sendMessage(Message.raw(i18nManager.translate("no_permission")));
            return;
        }

        String serverName = newServerName != null ? newServerName.trim() : "";
        String serverAddress = newServerAddress != null ? newServerAddress.trim() : "";
        if (serverName.isEmpty() || serverAddress.isEmpty()) {
            return;
        }

        String serverHost = serverAddress;
        int serverPort = DEFAULT_SERVER_PORT;
        int portSeparatorIndex = serverAddress.lastIndexOf(':');
        if (portSeparatorIndex > 0 && portSeparatorIndex < serverAddress.length() - 1) {
            serverHost = serverAddress.substring(0, portSeparatorIndex).trim();
            try {
                serverPort = Integer.parseInt(serverAddress.substring(portSeparatorIndex + 1).trim());
            } catch (NumberFormatException e) {
                return;
            }
        }

        if (serverHost.isEmpty()) {
            return;
        }

        if (!serverManager.addServer(new ServerEntry(serverName, serverHost, serverPort))) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        populateServerList(commandBuilder, eventBuilder, true);
        commandBuilder.set("#NewServerName.Value", "");
        commandBuilder.set("#NewServerIp.Value", "");
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    /**
     * Validates admin permission, removes the server, and refreshes the list UI.
     *
     * @param player player requesting deletion
     * @param ref reference to the entity store
     * @param store entity component store
     * @param serverNameToDelete name selected in the UI
     */
    private void handleDeleteServerRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
            String serverNameToDelete) {
        if (!player.hasPermission("swapserver.admin")) {
            player.sendMessage(Message.raw(i18nManager.translate("no_permission")));
            return;
        }

        if (serverNameToDelete == null || serverNameToDelete.trim().isEmpty()) {
            return;
        }

        serverManager.removeServer(serverNameToDelete.trim());

        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        populateServerList(commandBuilder, eventBuilder, true);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    /**
     * Closes the current page and requests a transfer to the target server.
     *
     * @param player player being transferred
     * @param target target server entry
     * @param ref reference to the entity store
     * @param store entity component store
     */
    private void transferToServer(Player player, ServerEntry target, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);
        playerRef.referToServer(target.getIp(), target.getPort());
    }

    public static class SwapServerPageEventData {
        static final String KEY_SERVER_NAME = "ServerName";
        static final String KEY_DELETE_SERVER_NAME = "DeleteServerName";
        static final String KEY_NEW_SERVER_NAME = "@NewServerName";
        static final String KEY_NEW_SERVER_IP = "@NewServerIp";

        public static final BuilderCodec<SwapServerPageEventData> CODEC = BuilderCodec
                .builder(SwapServerPageEventData.class, SwapServerPageEventData::new)
                .append(new KeyedCodec<>(KEY_SERVER_NAME, Codec.STRING),
                        (entry, value) -> entry.joinServerName = value, entry -> entry.joinServerName)
                .add()
                .append(new KeyedCodec<>(KEY_DELETE_SERVER_NAME, Codec.STRING),
                        (entry, value) -> entry.serverNameToDelete = value, entry -> entry.serverNameToDelete)
                .add()
                .append(new KeyedCodec<>(KEY_NEW_SERVER_NAME, Codec.STRING),
                        (entry, value) -> entry.newServerNameInput = value, entry -> entry.newServerNameInput)
                .add()
                .append(new KeyedCodec<>(KEY_NEW_SERVER_IP, Codec.STRING),
                        (entry, value) -> entry.newServerAddressInput = value,
                        entry -> entry.newServerAddressInput)
                .add()
                .build();

        private String joinServerName;
        private String serverNameToDelete;
        private String newServerNameInput;
        private String newServerAddressInput;
    }
}
