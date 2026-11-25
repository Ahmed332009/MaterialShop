package com.banglaverse.materialshop;

import org.bukkit.plugin.java.JavaPlugin;

/** Main plugin entry point for the MaterialShop barter marketplace. */
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
