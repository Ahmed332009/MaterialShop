package dev.materialshop;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** MaterialShop entry point. Companion to MaterialClaim; no Vault, no economy, no money. */
public final class MaterialShop extends JavaPlugin {
    private SettingsService settings;
    private Messages messages;
    private ShopStorage storage;
    private ShopManager shops;
    private GuiManager gui;
    private ChatPromptService prompts;
    private TradeService trade;
    private TradeLogger logs;
    private TerritoryHook territories;
    private ShopCommand shopCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new SettingsService(this);
        messages = new Messages(this);
        storage = new ShopStorage(this);
        storage.load();
        shops = new ShopManager(this);
        gui = new GuiManager(this);
        prompts = new ChatPromptService(this);
        trade = new TradeService(this);
        logs = new TradeLogger(this);
        territories = new TerritoryHook(this);
        shopCommand = new ShopCommand(this);
        ShopAdminCommand adminCommand = new ShopAdminCommand(this);

        var manager = getServer().getPluginManager();
        manager.registerEvents(gui, this);
        manager.registerEvents(prompts, this);
        manager.registerEvents(shopCommand, this);
        manager.registerEvents(new ShopEntityListener(this), this);
        manager.registerEvents(new ProtectionListener(this), this);

        command("shop", shopCommand, shopCommand);
        command("shopadmin", adminCommand, adminCommand);

        getLogger().info("MaterialShop " + getPluginMeta().getVersion() + " enabled ("
                + storage.all().size() + " shops loaded).");
    }

    private void command(String name, org.bukkit.command.CommandExecutor executor,
                         org.bukkit.command.TabCompleter tab) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }
        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(tab);
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

    public GuiManager gui() {
        return gui;
    }

    public ChatPromptService prompts() {
        return prompts;
    }

    public TradeService trade() {
        return trade;
    }

    public TradeLogger logs() {
        return logs;
    }

    public TerritoryHook territories() {
        return territories;
    }
}
