# Swap_Server

## Description
**Swap_Server** is a Hytale server plugin that allows players to easily switch between servers (cross-server teleportation).

It enables administrators to manage a list of servers (Lobby, Survival, Creative, etc.) and allows players to connect to them via a simple command. The plugin also handles saving player positions when they switch servers.

## Features
- **Fast Teleportation**: Switch servers with a simple command.
- **Server Management**: Add, modify, or delete servers directly in-game (Admin).
- **"Home" Point**: Set a main server (e.g., Lobby) to return to quickly.
- **Position Saving**: Your position is saved before every transfer.

---

## Commands

The main command is `/sws`. Here is the list of available commands and how to use them.

### For Players

#### Teleport to a server
Connect to a specific server by its name.
- **Command:** `/sws <server_name>` 
- **Example:** `/sws survival`
- **Description:** Teleports you to the server named "survival".

#### Return to main server (Home)
Quickly return to the server defined as main.
- **Command:** `/sws home`
- **Description:** Teleports you to the "Main" server.

#### List available servers
Displays the list of all configured servers.
- **Command:** `/sws list`
- **Description:** Displays a numbered list of servers with their names.

---

### For Administrators (Configuration)

These commands require the `swapserver.admin` permission.

#### Add a server
Adds a new server to the list.
- **Command:** `/sws add [main] <ip> <port> <name>`
- **Arguments:**
    - `[main]` (Optional): If you write "main", this server will become the main server.
    - `<ip>`: The target server IP address.
    - `<port>`: The target server port.
    - `<name>`: The unique name to identify this server.
- **Example:** `/sws add 192.168.1.50 3000 lobby`
- **Example (Set as main):** `/sws add main 192.168.1.50 3000 lobby`

#### Modify a server
Modifies an existing server's information. You need to know its index (visible via `/sws list`).
- **Command:** `/sws modify <index> <ip> <port> <name> [main]`
- **Arguments:**
    - `<index>`: The server number in the list (1, 2, 3...).
    - `<ip>`: The new IP address.
    - `<port>`: The new port.
    - `<name>`: The new name.
    - `[main]` (Optional): `true` to set as main, `false` otherwise.
- **Example:** `/sws modify 1 127.0.0.1 25565 hub true`

#### Delete a server
Removes a server from the list.
- **Command:** `/sws delete <name>`
- **Arguments:**
    - `<name>`: The name of the server to delete.
- **Example:** `/sws delete test_server`
*Note: This command will ask for confirmation.*

#### Confirm / Cancel an action
Used to confirm sensitive actions like deletion.
- **Confirm:** `/sws y`
- **Cancel:** `/sws n`

---

## Installation

1. Place the plugin structure in your server mods/plugins folder.
2. Ensure the configuration folder `mods/Swap_Server/data` is writable.
3. Start your server.
