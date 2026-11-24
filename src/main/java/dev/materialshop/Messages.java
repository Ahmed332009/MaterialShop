package dev.materialshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Every player-visible string lives in messages.yml. Placeholders use {name}. */
public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final MaterialShop plugin;
    private YamlConfiguration yaml;

    public Messages(MaterialShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    private String apply(String raw, String[] kv) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace("{" + kv[i] + "}", MM.escapeTags(kv[i + 1] == null ? "" : kv[i + 1]));
        }
        return out;
    }

    /** Raw MiniMessage source for key, including the configured prefix. */
    public String raw(String key, String... kv) {
        String prefix = yaml.getString("prefix", "<dark_green>[Shop]</dark_green> ");
        return prefix + apply(yaml.getString(key, "<red>Missing message: " + key), kv);
    }

    public Component get(String key, String... kv) {
        return MM.deserialize(raw(key, kv));
    }

    /** Same as get(), with no prefix: used for chest-menu titles and lore lines. */
    public Component plain(String key, String... kv) {
        return MM.deserialize(apply(yaml.getString(key, key), kv));
    }

    public String source(String key, String... kv) {
        return apply(yaml.getString(key, key), kv);
    }

    public List<Component> lore(String key, String... kv) {
        List<Component> out = new ArrayList<>();
        for (String line : yaml.getStringList(key)) {
            out.add(plainLine(apply(line, kv)));
        }
        return out;
    }

    private Component plainLine(String mini) {
        return MM.deserialize(mini);
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(get(key, kv));
    }

    /** Sends a MiniMessage string built from messages.yml fragments, with the prefix. */
    public void sendMini(CommandSender to, String mini) {
        to.sendMessage(MM.deserialize(yaml.getString("prefix", "<dark_green>[Shop]</dark_green> ") + mini));
    }

    public Component mini(String value) {
        return MM.deserialize(value);
    }

    public String escape(String value) {
        return MM.escapeTags(value == null ? "" : value);
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

    public List<String> list(String key) {
        return yaml.getStringList(key);
    }

    public boolean bool(String key, boolean fallback) {
        return yaml.getBoolean(key, fallback);
    }
}
