package com.dmvlab.swapserver.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;

import com.dmvlab.swapserver.manager.ConfigManager;
import com.dmvlab.swapserver.manager.PositionManager;
import com.dmvlab.swapserver.manager.ServerManager;
import com.dmvlab.swapserver.model.ServerEntry;
import com.dmvlab.swapserver.net.TransferPayload;
import com.dmvlab.swapserver.i18n.I18nManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.dmvlab.swapserver.model.PlayerPosition;

public class SwsCommand extends CommandBase {
    private final ServerManager serverManager;
    private final PositionManager positionManager;
    private final ConfigManager configManager;
    private final I18nManager i18nManager;

    public SwsCommand(ServerManager sm, PositionManager pm, ConfigManager cm, I18nManager im) {
        super("sws", "Swap Server Command");
        setAllowsExtraArguments(true);
        this.serverManager = sm;
        this.positionManager = pm;
        this.configManager = cm;
        this.i18nManager = im;

        // Register subcommands (keep admin / utility subcommands)
        addSubCommand(new ListCommand("list", "List available servers w/ indexes (Usage: /sws list)"));
        addSubCommand(new AddCommand("add", "Add a new server (Usage: /sws add [main] <ip> <port> <name>)"));
        addSubCommand(new DeleteCommand("delete", "Delete a server (Usage: /sws delete <name>)"));
        addSubCommand(
                new ModifyCommand("modify", "Modify a server (Usage: /sws modify <index> <ip> <port> <name> [main])"));
        addSubCommand(new HomeCommand("home", "Teleport to main server (Usage: /sws home)"));
        // Note: TeleportCommand removed. Teleport behaviour is now the default action
        // of /sws <name>
        addSubCommand(new YesCommand("y", "Confirm action (Usage: /sws y)"));
        addSubCommand(new NoCommand("n", "Cancel action (Usage: /sws n)"));
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        CommandSender sender = ctx.sender();
        String input = ctx.getInputString();
        String[] tokens = tokenize(input);

        // Determine the index of the first "argument" (the token after the command name
        // if present)
        int firstArgIndex = findFirstArgIndex(tokens, this.getName());

        // If a first argument exists and is not one of the subcommand names, try
        // teleport shorthand
        if (firstArgIndex != -1 && firstArgIndex < tokens.length) {
            String potential = tokens[firstArgIndex];
            if (!isSubCommandName(potential)) {
                if (serverManager.getByName(potential).isPresent()) {
                    if (!checkCooldown(sender))
                        return;
                    try {
                        handleTeleport(sender, potential, ctx);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // If no shorthand matched, show usage help
        sender.sendMessage(Message.raw("Usage Shortcuts:"));
        sender.sendMessage(Message.raw("  /sws <name> : Teleport to the server named <name>"));
        sender.sendMessage(Message.raw("Commands:"));
        sender.sendMessage(Message.raw("  /sws list : List servers with indexes"));
        sender.sendMessage(Message.raw("  /sws add [main] ... : Add a server"));
        sender.sendMessage(Message.raw("  /sws modify <index> ... : Modify a server"));
        sender.sendMessage(Message.raw("  /sws delete <name>"));
        sender.sendMessage(Message.raw("  /sws home"));
    }

    private boolean checkCooldown(CommandSender sender) {
        if (!ctxIsPlayer(sender))
            return true; // Console has no cooldown
        UUID uuid = sender.getUuid();
        if (uuid == null)
            return true; // defensive
        if (!CooldownManager.canUse(uuid, configManager.getCommandCooldownSeconds())) {
            sender.sendMessage(Message.raw(i18nManager.tr("cooldown_wait")));
            return false;
        }
        return true;
    }

    private boolean ctxIsPlayer(CommandSender sender) {
        return sender.getUuid() != null;
    }

    // helper: tokenize input (trim, split on whitespace, remove empties)
    private String[] tokenize(String input) {
        if (input == null || input.isEmpty())
            return new String[0];
        String[] raw = input.trim().split("\\s+");
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s != null && !s.isEmpty())
                out.add(s);
        }
        return out.toArray(new String[0]);
    }

    // helper: find index of first user-supplied argument (token after command name
    // if present)
    private int findFirstArgIndex(String[] tokens, String commandName) {
        if (tokens == null || tokens.length == 0)
            return -1;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.equalsIgnoreCase(commandName) || t.equalsIgnoreCase("/" + commandName)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    // helper: prevents treating subcommand names as server names for shorthand
    private boolean isSubCommandName(String token) {
        if (token == null)
            return false;
        String t = token.toLowerCase();
        switch (t) {
            case "list":
            case "add":
            case "delete":
            case "modify":
            case "home":
            case "y":
            case "n":
                return true;
            default:
                return false;
        }
    }

    // --- SubCommands ---

    private class ListCommand extends CommandBase {
        public ListCommand(String name, String desc) {
            super(name, desc);
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            if (!checkCooldown(ctx.sender()))
                return;
            CommandSender sender = ctx.sender();
            sender.sendMessage(Message.raw("Servers:"));
            int i = 1;
            for (ServerEntry se : serverManager.list()) {
                String prefix = se.isMain() ? "[Main] " : "";
                sender.sendMessage(Message.raw(i + ". " + prefix + se.getName()));
                i++;
            }
        }
    }

    private class AddCommand extends CommandBase {
        public AddCommand(String name, String desc) {
            super(name, desc);
            requirePermission("swapserver.admin");
            setAllowsExtraArguments(true);
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");

            if (args.length < 5) {
                sender.sendMessage(Message.raw("Usage: /sws add [main] <ip> <port> <name>"));
                return;
            }

            boolean isMain = args[2].equalsIgnoreCase("main");
            int offset = isMain ? 1 : 0;

            if (args.length < 5 + offset) {
                sender.sendMessage(Message.raw("Invalid args."));
                return;
            }

            String ip = args[2 + offset];
            String portStr = args[3 + offset];
            String name = args[4 + offset];

            try {
                int port = Integer.parseInt(portStr);

                if (isMain) {
                    serverManager.list().forEach(s -> s.setMain(false));
                }

                serverManager.add(new ServerEntry(name, ip, port, isMain));
                sender.sendMessage(Message.raw("Server added: " + name));
            } catch (NumberFormatException e) {
                sender.sendMessage(Message.raw("Invalid port number."));
            }
        }
    }

    private class DeleteCommand extends CommandBase {
        public DeleteCommand(String name, String desc) {
            super(name, desc);
            requirePermission("swapserver.admin");
            setAllowsExtraArguments(true);
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            if (args.length < 3) {
                sender.sendMessage(Message.raw("Usage: /sws delete <name>"));
                return;
            }
            String name = args[2];

            if (!serverManager.getByName(name).isPresent()) {
                sender.sendMessage(Message.raw("Server not found: " + name));
                return;
            }

            sender.sendMessage(Message.raw("Confirm deletion of '" + name + "' ? (/sws y or /sws n)"));
            ConfirmationManager.request(sender.getUuid(), () -> {
                serverManager.remove(name);
                sender.sendMessage(Message.raw("Server deleted: " + name));
            });
        }
    }

    private class ModifyCommand extends CommandBase {
        public ModifyCommand(String name, String desc) {
            super(name, desc);
            requirePermission("swapserver.admin");
            setAllowsExtraArguments(true);
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            // Usage: /sws modify <index> <ip> <port> <name> [main]
            // args indices: 0:sws 1:modify 2:index 3:ip 4:port 5:name 6:main(opt)
            if (args.length < 6) {
                sender.sendMessage(Message.raw("Usage: /sws modify <index> <ip> <port> <name> [main]"));
                return;
            }

            try {
                int index = Integer.parseInt(args[2]) - 1; // 1-based index from user
                java.util.List<ServerEntry> list = serverManager.list();

                if (index < 0 || index >= list.size()) {
                    sender.sendMessage(Message.raw("Invalid server index: " + (index + 1)));
                    return;
                }

                ServerEntry s = list.get(index);
                String oldName = s.getName();

                String newIp = args[3];
                int newPort = Integer.parseInt(args[4]);
                String newName = args[5];

                s.setIp(newIp);
                s.setPort(newPort);
                s.setName(newName);

                if (args.length >= 7) {
                    boolean isMain = Boolean.parseBoolean(args[6]);
                    if (isMain) {
                        list.forEach(x -> x.setMain(false));
                    }
                    s.setMain(isMain);
                }

                serverManager.save();
                sender.sendMessage(Message.raw("Modified server " + (index + 1) + " (" + oldName + " -> " + newName
                        + ")"));

            } catch (NumberFormatException e) {
                sender.sendMessage(Message.raw("Invalid number format for index or port."));
            } catch (Exception e) {
                sender.sendMessage(Message.raw("Error updating: " + e.getMessage()));
            }
        }
    }

    private class HomeCommand extends CommandBase {
        public HomeCommand(String name, String desc) {
            super(name, desc);
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            if (!checkCooldown(ctx.sender()))
                return;
            try {
                handleHome(ctx.sender(), ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class YesCommand extends CommandBase {
        public YesCommand(String name, String desc) {
            super(name, desc);
            requirePermission("swapserver.admin");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            if (!ConfirmationManager.confirm(ctx.sender().getUuid(), true)) {
                ctx.sender().sendMessage(Message.raw(i18nManager.tr("confirmation_none")));
            }
        }
    }

    private class NoCommand extends CommandBase {
        public NoCommand(String name, String desc) {
            super(name, desc);
            requirePermission("swapserver.admin");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            if (ConfirmationManager.confirm(ctx.sender().getUuid(), false)) {
                ctx.sender().sendMessage(Message.raw("Cancelled."));
            } else {
                ctx.sender().sendMessage(Message.raw(i18nManager.tr("confirmation_none")));
            }
        }
    }

    // --- Helper Methods ---
    private void handleHome(CommandSender sender, CommandContext ctx) throws Exception {
        Optional<ServerEntry> main = serverManager.list().stream().filter(ServerEntry::isMain).findFirst();
        if (!main.isPresent()) {
            sender.sendMessage(Message.raw("No main server."));
            return;
        }
        performTeleport(sender, main.get(), ctx);
    }

    private void handleTeleport(CommandSender sender, String name, CommandContext ctx) throws Exception {
        Optional<ServerEntry> opt = serverManager.getByName(name);
        if (!opt.isPresent()) {
            sender.sendMessage(Message.raw(i18nManager.tr("server_not_found", name)));
            return;
        }
        performTeleport(sender, opt.get(), ctx);
    }

    private void performTeleport(CommandSender sender, ServerEntry target, CommandContext ctx) throws Exception {

        if (!sender.getClass().getName().contains("Player")) {
        }

        PlayerRef player;
        if (sender instanceof PlayerRef) {
            player = (PlayerRef) sender;
        } else if (sender.getClass().getName().equals("com.hypixel.hytale.server.core.entity.entities.Player")) {

            com.hypixel.hytale.server.core.entity.entities.Player realPlayer = (com.hypixel.hytale.server.core.entity.entities.Player) sender;
            player = realPlayer.getPlayerRef();
        } else {
            sender.sendMessage(Message.raw("Only players can teleport. Sender class: " + sender.getClass().getName()));
            return;
        }

        String uuid = sender.getUuid().toString();

        // Save Position
        Vector3d pos = player.getTransform().getPosition();
        Vector3f rot = player.getHeadRotation();

        PlayerPosition pPos = new PlayerPosition(
                configManager.getServerName(),
                pos.x, pos.y, pos.z,
                rot.y, rot.x);
        positionManager.savePlayerPosition(uuid, configManager.getServerName(), pPos);

        Map<String, Object> payload = new HashMap<>();
        payload.put("uuid", uuid);
        payload.put("fromServer", configManager.getServerName());

        String secret = configManager.getTransferSecret();
        String packed = TransferPayload.createPayload(payload, secret);

        player.referToServer(target.getIp(), target.getPort(), packed.getBytes());
    }
}
