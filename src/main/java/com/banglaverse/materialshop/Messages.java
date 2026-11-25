package com.banglaverse.materialshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Loads messages.yml and turns &-color strings into Adventure Components. */
public final class Messages {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final MaterialShop plugin;
    private YamlConfiguration yaml;

    public Messages(MaterialShop plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reloads the configured language file, copying the shipped default if needed. */
    public void reload() {
        String name = plugin.getConfig().getString("language-file", "messages.yml");
        if (name == null || name.isBlank()) {
            name = "messages.yml";
        }
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not create " + name + ": " + ex.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    private String apply(String raw, String[] kv) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace("{" + kv[i] + "}", clean(kv[i + 1]));
        }
        return out;
    }

    /** Player text never injects color codes. */
    public String clean(String value) {
        return value == null ? "" : value.replace("&", "");
    }

    public Component component(String key, String... kv) {
        String prefix = yaml.getString("prefix", "&2[Shop]&r ");
        return LEGACY.deserialize(prefix + apply(yaml.getString(key, key), kv));
    }

    public Component plain(String key, String... kv) {
        return LEGACY.deserialize(apply(yaml.getString(key, key), kv));
    }

    public List<Component> lore(String key, String... kv) {
        List<Component> out = new ArrayList<>();
        for (String line : yaml.getStringList(key)) {
            out.add(LEGACY.deserialize(apply(line, kv)));
        }
        return out;
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(component(key, kv));
    }

    public void sendPlain(CommandSender to, String key, String... kv) {
        to.sendMessage(plain(key, kv));
    }

    public String string(String key, String fallback) {
        return yaml.getString(key, fallback);
    }

    public int integer(String key, int fallback) {
        return yaml.getInt(key, fallback);
    }

    public long longValue(String key, long fallback) {
        return yaml.getLong(key, fallback);
    }

    public boolean bool(String key, boolean fallback) {
        return yaml.getBoolean(key, fallback);
    }

    public List<String> list(String key) {
        return yaml.getStringList(key);
    }

    public FileConfiguration config() {
        return yaml;
    }
}
