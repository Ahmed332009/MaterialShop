package dev.materialshop;

import org.bukkit.plugin.java.JavaPlugin;

/** MaterialShop entry point. Stage 1 skeleton; services are wired in stage 2. */
public final class MaterialShop extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("MaterialShop " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MaterialShop disabled.");
    }
}
