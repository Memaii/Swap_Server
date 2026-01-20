package com.dmvlab.swapserver.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateTranslations;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class I18nManager {
    public static final String DEFAULT_LOCALE = "en_US";
    private final Map<String, String> translations;

    /**
     * Loads the translation map for the requested locale.
     *
     * @param locale locale code such as "en_US" or "fr_FR"
     */
    public I18nManager(String locale) {
        Map<String, String> loaded = new HashMap<>();
        String resolvedLocale = resolveLocale(locale);
        String resourcePath = "/lang/" + resolvedLocale + ".json";
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream != null) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                loaded = new Gson().fromJson(reader, mapType);
            } catch (Exception e) {
                System.err.println("Could not load language file: " + locale);
                e.printStackTrace();
            }
        } else {
            System.err.println("Could not find language file: " + locale);
        }
        this.translations = loaded != null ? loaded : new HashMap<>();
    }

    /**
     * Normalizes and resolves the locale to an existing language file.
     *
     * @param locale locale code requested by the player
     * @return a locale string that should exist in resources
     */
    private static String resolveLocale(String locale) {
        String normalized = normalizeLocale(locale);
        if (normalized == null || normalized.isEmpty()) {
            return DEFAULT_LOCALE;
        }
        if (languageFileExists(normalized)) {
            return normalized;
        }
        if (!DEFAULT_LOCALE.equals(normalized) && languageFileExists(DEFAULT_LOCALE)) {
            return DEFAULT_LOCALE;
        }
        return normalized;
    }

    /**
     * Converts locale strings like "en-US" to "en_US".
     *
     * @param locale locale code from the player or client
     * @return normalized locale or null if input is empty
     */
    private static String normalizeLocale(String locale) {
        if (locale == null) {
            return null;
        }
        String trimmed = locale.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed.replace('-', '_');
        String[] parts = normalized.split("_", 2);
        if (parts.length == 2) {
            return parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
        }
        return normalized.toLowerCase();
    }

    /**
     * Checks whether a language file exists in resources.
     *
     * @param locale locale code to check
     * @return true if the resource file is present
     */
    private static boolean languageFileExists(String locale) {
        return I18nManager.class.getResource("/lang/" + locale + ".json") != null;
    }

    /**
     * Returns the translated message for the key and formats it with arguments.
     *
     * @param key translation key
     * @param args optional arguments for MessageFormat
     * @return the translated and formatted string
     */
    public String translate(String key, Object... args) {
        String message = translations.getOrDefault(key, key);
        return MessageFormat.format(message, args);
    }

    /**
     * Returns a copy of the loaded translation map.
     *
     * @return a map of translation keys to localized strings
     */
    public Map<String, String> getTranslations() {
        return new HashMap<>(translations);
    }

    /**
     * Sends the translations to a player so the client can resolve keys.
     *
     * @param playerRef the player to update
     */
    public void sendTo(PlayerRef playerRef) {
        if (playerRef == null || translations.isEmpty()) {
            return;
        }
        playerRef.getPacketHandler().writeNoCache(
                new UpdateTranslations(UpdateType.AddOrUpdate, new HashMap<>(translations)));
    }
}
