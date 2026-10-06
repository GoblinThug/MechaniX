package org.goblinthug.mechanix;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LangManager {

    private final MechaniX plugin;
    private FileConfiguration lang;
    private String prefix = "";

    public LangManager(MechaniX plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String locale = plugin.getConfig().getString("general.locale", "ru").toLowerCase();

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        File external = new File(langDir, locale + ".yml");
        if (!external.exists()) {
            if (plugin.getResource("lang/" + locale + ".yml") != null) {
                plugin.saveResource("lang/" + locale + ".yml", false);
            } else {
                plugin.getLogger().warning("Locale '" + locale + "' not found, falling back to 'ru'.");
                locale = "ru";
                external = new File(langDir, locale + ".yml");
                if (!external.exists()) plugin.saveResource("lang/ru.yml", false);
            }
        }

        lang = YamlConfiguration.loadConfiguration(external);

        InputStream def = plugin.getResource("lang/" + locale + ".yml");
        if (def != null) {
            YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(def, StandardCharsets.UTF_8));
            lang.setDefaults(defCfg);
        }

        prefix = color(lang.getString("prefix", ""));
        plugin.getLogger().info("Locale loaded: " + locale);
    }

    public String raw(String key) {
        String s = lang.getString(key);
        if (s == null) return "&c<" + key + ">";
        return color(s);
    }

    public String get(String key) {
        return prefix + raw(key);
    }

    public String get(String key, Object... placeholders) {
        String result = raw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return prefix + result;
    }

    public String prefix() {
        return prefix;
    }

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}