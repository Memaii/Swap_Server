package com.dmvlab.swapserver.i18n;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;

public class I18nManager {
    private final Map<String, String> messages;

    @SuppressWarnings("unchecked")
    public I18nManager(String lang) {
        Map<String, String> loaded = new HashMap<>();
        try {
            loaded = new Gson().fromJson(
                    new InputStreamReader(getClass().getResourceAsStream("/lang/" + lang + ".json")),
                    Map.class);
        } catch (Exception e) {
            System.err.println("Could not load language file: " + lang);
            e.printStackTrace();
        }
        this.messages = loaded != null ? loaded : new HashMap<>();
    }

    public String tr(String key, Object... args) {
        String msg = messages.getOrDefault(key, key);
        return MessageFormat.format(msg, args);
    }
}
