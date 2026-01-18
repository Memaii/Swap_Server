package com.dmvlab.swapserver.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Path configPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Object> config = new HashMap<>();

    public ConfigManager(Path dataFolder) {
        this.configPath = dataFolder.resolve("config.json");
        try {
            if (!Files.exists(dataFolder))
                Files.createDirectories(dataFolder);
            if (!Files.exists(configPath)) {
                // default config
                config.put("serverName", "lobby");
                config.put("transferSecret", "please-change-this-secret");
                config.put("commandCooldownSeconds", 5);
                Files.write(configPath, gson.toJson(config).getBytes());
            } else {
                String txt = new String(Files.readAllBytes(configPath));
                config = gson.fromJson(txt, Map.class);
                if (config == null)
                    config = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerName() {
        Object v = config.get("serverName");
        return v != null ? v.toString() : "unknown";
    }

    public String getTransferSecret() {
        Object v = config.get("transferSecret");
        return v != null ? v.toString() : "";
    }

    public int getCommandCooldownSeconds() {
        return getInt("commandCooldownSeconds", 5);
    }

    public int getInt(String key, int def) {
        Object v = config.get(key);
        if (v instanceof Number)
            return ((Number) v).intValue();
        return def;
    }

    public void set(String key, Object value) {
        config.put(key, value);
    }

    public void save() {
        try {
            Files.write(configPath, gson.toJson(config).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
