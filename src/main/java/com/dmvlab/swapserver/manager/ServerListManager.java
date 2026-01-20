package com.dmvlab.swapserver.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dmvlab.swapserver.model.ServerEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerListManager {
    private final Path serversFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<ServerEntry> serverEntries = new ArrayList<>();

    /**
     * Creates a manager and loads or creates the servers.json file.
     *
     * @param dataFolder folder where the servers.json file is stored
     */
    public ServerListManager(Path dataFolder) {
        this.serversFile = dataFolder.resolve("servers.json");
        try {
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
            if (!Files.exists(serversFile)) {
                saveServers();
            } else {
                String json = new String(Files.readAllBytes(serversFile), StandardCharsets.UTF_8);
                ServerEntry[] loaded = gson.fromJson(json, ServerEntry[].class);
                if (loaded != null) {
                    for (ServerEntry entry : loaded) {
                        if (entry == null) {
                            continue;
                        }
                        serverEntries.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the current list of server entries.
     *
     * @return the list of servers currently in memory
     */
    public List<ServerEntry> getServerList() {
        return serverEntries;
    }

    /**
     * Finds a server by name using a case-insensitive match.
     *
     * @param name the server name to search for
     * @return an optional server entry if found
     */
    public Optional<ServerEntry> findServerByName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return serverEntries.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Adds a server entry and persists the list to disk.
     *
     * @param entry the server entry to add
     * @return true if the server was added, false if invalid or duplicate
     */
    public boolean addServer(ServerEntry entry) {
        if (entry == null || entry.getName() == null || entry.getName().isEmpty()) {
            return false;
        }
        if (findServerByName(entry.getName()).isPresent()) {
            return false;
        }
        serverEntries.add(entry);
        saveServers();
        return true;
    }

    /**
     * Removes any server entries matching the provided name and persists.
     *
     * @param name the server name to remove (case-insensitive)
     */
    public void removeServer(String name) {
        serverEntries.removeIf(s -> s.getName().equalsIgnoreCase(name));
        saveServers();
    }

    /**
     * Writes the current server list to servers.json.
     */
    private void saveServers() {
        try {
            Files.write(serversFile, gson.toJson(serverEntries).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
