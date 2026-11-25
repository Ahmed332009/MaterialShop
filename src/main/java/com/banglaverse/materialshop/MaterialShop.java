package com.banglaverse.materialshop;

import com.banglaverse.materialshop.chest.ChestShopListener;
import com.banglaverse.materialshop.chest.ChestShopManager;
import com.banglaverse.materialshop.command.ShopAdminCommand;
import com.banglaverse.materialshop.command.ShopCommand;
import com.banglaverse.materialshop.listener.MenuListener;
import com.banglaverse.materialshop.listener.NpcListener;
import com.banglaverse.materialshop.listener.PromptManager;
import com.banglaverse.materialshop.menu.MenuBase;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.storage.DataStore;
import com.banglaverse.materialshop.trade.Rewards;
import com.banglaverse.materialshop.trade.TradeEngine;
import com.banglaverse.materialshop.trade.TradeLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

/** Main plugin entry point for the MaterialShop barter marketplace. */
public final class MaterialShop extends JavaPlugin {
    private Settings settings;
    private Messages messages;
    private DataStore data;
    private PromptManager prompts;
    private TradeEngine trade;
    private TradeLogger logs;
    private ChestShopManager chests;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(this);
        messages = new Messages(this);
        data = new DataStore(this);
        data.load();
        prompts = new PromptManager(this);
        trade = new TradeEngine(this);
        logs = new TradeLogger(this);
        chests = new ChestShopManager(this);

        var manager = getServer().getPluginManager();
        manager.registerEvents(new MenuListener(), this);
        manager.registerEvents(prompts, this);
        manager.registerEvents(new NpcListener(this), this);
        manager.registerEvents(new ChestShopListener(this), this);

        command("shop", new ShopCommand(this));
        command("shopadmin", new ShopAdminCommand(this));

        getServer().getScheduler().runTaskTimer(this, this::expireListings, 20L * 60L, 20L * 60L);
        getLogger().info("MaterialShop " + getPluginMeta().getVersion() + " enabled.");
    }

    private void command(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }
        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter((org.bukkit.command.TabCompleter) executor);
    }

    private void expireListings() {
        if (settings.expiryDays() <= 0) {
            return;
        }
        for (Listing listing : new ArrayList<>(data.listings().values())) {
            if (!listing.expired()) {
                continue;
            }
            Rewards.give(this, listing.owner(), listing.escrow());
            data.removeListing(listing.id());
            Player owner = Bukkit.getPlayer(listing.owner());
            if (owner != null && owner.isOnline()) {
                msg().send(owner, "listing-expired");
            }
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuBase menu) {
                menu.onClose();
            }
        }
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

    public PromptManager prompts() {
        return prompts;
    }

    public TradeEngine trade() {
        return trade;
    }

    public TradeLogger logs() {
        return logs;
    }

    public ChestShopManager chests() {
        return chests;
    }
}
