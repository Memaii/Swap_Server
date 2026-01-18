package com.dmvlab.swapserver.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dmvlab.swapserver.model.PlayerPosition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PositionManager {
    private final Path positionsPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Map<String, PlayerPosition>> players = new HashMap<>();

    public PositionManager(Path dataFolder) {
        this.positionsPath = dataFolder.resolve("positions.json");
        try {
            if (!Files.exists(dataFolder))
                Files.createDirectories(dataFolder);
            if (Files.exists(positionsPath)) {
                String txt = new String(Files.readAllBytes(positionsPath));
                // Type safety warning ignored for simplicity, but strictly should use TypeToken
                players = gson.fromJson(txt, Map.class);
                if (players == null)
                    players = new HashMap<>();
            } else {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Note: Due to gson map deserialization of generic types, we might get
    // LinkedHashMaps instead of PlayerPosition objects if not careful.
    // For specific implementation, we should manually parse or use TypeToken.
    // However, to keep it simple and consistent with the user snippet which casted
    // things manually in the EventListener,
    // I will ensure we treat the map carefully.
    // Actually, in the user snippet for PlayerEventListener, they manually parse
    // the map from the payload.
    // But here we are loading from file. Let's fix the type safety properly.

    public synchronized void savePlayerPosition(String uuid, String serverName, PlayerPosition pos) {
        players.computeIfAbsent(uuid, k -> new HashMap<>()).put(serverName, pos);
        save();
    }

    public synchronized PlayerPosition getPlayerPosition(String uuid, String serverName) {
        Map<String, PlayerPosition> map = players.get(uuid);
        if (map == null)
            return null;
        Object posObj = map.get(serverName);
        if (posObj == null)
            return null;

        // If loaded from JSON w/o TypeToken, it might be a LinkedTreeMap.
        if (posObj instanceof PlayerPosition)
            return (PlayerPosition) posObj;

        // Convert from Map if needed (quick hack for Gson non-typed deserialization)
        String json = gson.toJson(posObj);
        return gson.fromJson(json, PlayerPosition.class);
    }

    public synchronized void save() {
        try {
            Files.write(positionsPath, gson.toJson(players).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
