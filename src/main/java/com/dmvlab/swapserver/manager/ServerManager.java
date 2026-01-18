package com.dmvlab.swapserver.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dmvlab.swapserver.model.ServerEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerManager {
    private final Path serversPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<ServerEntry> servers = new ArrayList<>();

    public ServerManager(Path dataFolder) {
        this.serversPath = dataFolder.resolve("servers.json");
        try {
            if (!Files.exists(dataFolder))
                Files.createDirectories(dataFolder);
            if (!Files.exists(serversPath)) {
                // default servers
                servers.add(new ServerEntry("main", "127.0.0.1", 5520, true));
                save();
            } else {
                String txt = new String(Files.readAllBytes(serversPath));
                ServerEntry[] arr = gson.fromJson(txt, ServerEntry[].class);
                if (arr != null)
                    for (ServerEntry s : arr)
                        servers.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ServerEntry> list() {
        return servers;
    }

    public Optional<ServerEntry> getByName(String name) {
        return servers.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void add(ServerEntry entry) {
        servers.add(entry);
        save();
    }

    public void remove(String name) {
        servers.removeIf(s -> s.getName().equalsIgnoreCase(name));
        save();
    }

    public void save() {
        try {
            Files.write(serversPath, gson.toJson(servers).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
