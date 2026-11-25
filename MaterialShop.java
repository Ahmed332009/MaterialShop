package com.banglaverse.materialshop;

import com.banglaverse.materialshop.storage.DataStore;
import org.bukkit.plugin.java.JavaPlugin;

/** Main plugin entry point for the MaterialShop barter marketplace. */
public final class MaterialShop extends JavaPlugin {
    private Settings settings;
    private Messages messages;
    private DataStore data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(this);
        messages = new Messages(this);
        data = new DataStore(this);
        data.load();
        getLogger().info("MaterialShop " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (data != null) {
            data.save();
        }
        getLogger().info("MaterialShop disabled; data saved.");
    }

    /** Applies config.yml and messages.yml changes without a restart. */
    public void reloadAll() {
        settings.reload();
        messages.reload();
    }

    public Settings settings() {
        return settings;
    }

    public Messages msg() {
        return messages;
    }

    public DataStore data() {
        return data;
    }
}
