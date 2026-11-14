package com.afox.headball;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class Messages {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "ru");
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    private final HeadBall plugin;
    private YamlConfiguration config = new YamlConfiguration();
    private String language = DEFAULT_LANGUAGE;

    public Messages(HeadBall plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String configured = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        String code = configured == null ? DEFAULT_LANGUAGE : configured.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_LANGUAGES.contains(code)) {
            plugin.getLogger().warning("Unknown language '" + configured + "', using " + DEFAULT_LANGUAGE + ".");
            code = DEFAULT_LANGUAGE;
        }

        for (String supported : SUPPORTED_LANGUAGES) {
            saveLanguage(supported);
        }

        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream stream = plugin.getResource("lang/" + code + ".yml");
        if (stream != null) {
            loaded.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ));
        }

        this.config = loaded;
        this.language = code;
    }

    private void saveLanguage(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + code + ".yml", false);
        }
    }

    public String get(String key, Object... placeholders) {
        return raw(key, placeholders).replace('&', '§');
    }

    public String plain(String key, Object... placeholders) {
        return COLOR_PATTERN.matcher(raw(key, placeholders)).replaceAll("");
    }

    private String raw(String key, Object... placeholders) {
        String value = config.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Missing language key '" + key + "' in " + language + ".yml");
            return key;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return value;
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }
}
