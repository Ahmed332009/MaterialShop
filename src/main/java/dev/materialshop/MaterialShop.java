package dev.materialshop;

import org.bukkit.plugin.java.JavaPlugin;

/** MaterialShop entry point. Companion to MaterialClaim; no Vault, no economy, no money. */
public final class MaterialShop extends JavaPlugin {
    private SettingsService settings;
    private Messages messages;
    private ShopStorage storage;
    private ShopManager shops;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new SettingsService(this);
        messages = new Messages(this);
        storage = new ShopStorage(this);
        storage.load();
        shops = new ShopManager(this);
        getLogger().info("MaterialShop " + getPluginMeta().getVersion() + " enabled ("
                + storage.all().size() + " shops loaded).");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.save();
        }
        getLogger().info("MaterialShop disabled; shop data saved.");
    }

    public void reloadAll() {
        settings.reload();
        messages.reload();
    }

    public SettingsService settings() {
        return settings;
    }

    public Messages msg() {
        return messages;
    }

    public ShopStorage storage() {
        return storage;
    }

    public ShopManager shops() {
        return shops;
    }
}
