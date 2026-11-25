package com.banglaverse.materialshop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads and caches config.yml: limits, payment rules, categories, logs and expiry. */
public final class Settings {
    private static final List<String> CATEGORY_ORDER = List.of(
            "Blocks", "Tools and Weapons", "Armor", "Food", "Redstone", "Farming",
            "Mob drops", "Ores and Ingots", "Misc", "Custom items");

    private final MaterialShop plugin;
    private int shopLimit = 5;
    private int maxListings = 20;
    private long expiryDays = 0;
    private int maxQuantity = 2304;
    private int collectCapacity = 45;
    private int promptTicks = 600;
    private boolean whitelistEnabled = false;
    private final Set<String> blacklist = new HashSet<>();
    private final Set<String> whitelist = new HashSet<>();
    private final Map<Material, Double> values = new HashMap<>();
    private final Map<Material, String> categories = new HashMap<>();
    private final List<String> categoryNames = new ArrayList<>();
    private double fairnessRatio = 4.0;
    private boolean chestEnabled = true;
    private boolean logEnabled = true;
    private long rotateBytes = 5L * 1024L * 1024L;
    private int logLines = 12;

    public Settings(MaterialShop plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reloads every config value without a server restart. */
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        shopLimit = Math.max(1, cfg.getInt("shop-limit", 5));
        maxListings = Math.max(1, cfg.getInt("max-listings", 20));
        expiryDays = Math.max(0, cfg.getLong("listing-expiry-days", 0));
        maxQuantity = Math.max(1, cfg.getInt("max-quantity", 2304));
        collectCapacity = Math.max(1, cfg.getInt("collect-box-capacity", 45));
        promptTicks = Math.max(5, cfg.getInt("prompt-timeout-seconds", 30)) * 20L;
        whitelistEnabled = cfg.getBoolean("payment-whitelist-enabled", false);
        blacklist.clear();
        whitelist.clear();
        for (String s : cfg.getStringList("payment-blacklist")) {
            blacklist.add(normalize(s));
        }
        for (String s : cfg.getStringList("payment-whitelist")) {
            whitelist.add(normalize(s));
        }
        values.clear();
        ConfigurationSection valueSec = cfg.getConfigurationSection("material-values");
        if (valueSec != null) {
            for (String key : valueSec.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                double value = valueSec.getDouble(key, 0.0);
                if (material != null && value > 0) {
                    values.put(material, value);
                }
            }
        }
        categories.clear();
        categoryNames.clear();
        ConfigurationSection categorySec = cfg.getConfigurationSection("categories");
        if (categorySec != null) {
            List<String> names = new ArrayList<>(categorySec.getKeys(false));
            names.sort((a, b) -> {
                int ia = CATEGORY_ORDER.indexOf(a);
                int ib = CATEGORY_ORDER.indexOf(b);
                return Integer.compare(ia < 0 ? 100 : ia, ib < 0 ? 100 : ib);
            });
            for (String name : names) {
                categoryNames.add(name);
                for (String materialName : categorySec.getStringList(name)) {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        categories.put(material, name);
                    }
                }
            }
        }
        if (categoryNames.isEmpty()) {
            categoryNames.addAll(CATEGORY_ORDER);
        }
        fairnessRatio = Math.max(0.0, cfg.getDouble("fairness-ratio", 4.0));
        chestEnabled = cfg.getBoolean("chest-shops.enabled", true);
        logEnabled = cfg.getBoolean("logging.enabled", true);
        rotateBytes = Math.max(1L, cfg.getLong("logging.rotate-megabytes", 5L)) * 1024L * 1024L;
        logLines = Math.max(1, cfg.getInt("logging.log-lines-in-gui", 12));
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('.', '_');
    }

    public int shopLimit() {
        return shopLimit;
    }

    /** Chest-shop limit: config default, raised by the highest materialshop.limit.&lt;n&gt;. */
    public int shopLimit(Player player) {
        if (player.hasPermission("materialshop.admin.bypass")) {
            return Integer.MAX_VALUE;
        }
        int limit = shopLimit;
        for (var info : player.getEffectivePermissions()) {
            if (!info.getValue() || !info.getPermission().startsWith("materialshop.limit.")) {
                continue;
            }
            try {
                limit = Math.max(limit, Integer.parseInt(
                        info.getPermission().substring("materialshop.limit.".length())));
            } catch (NumberFormatException ignored) {
                // Not a numeric limit node.
            }
        }
        return limit;
    }

    public int maxListings() {
        return maxListings;
    }

    public long expiryDays() {
        return expiryDays;
    }

    public long expiryMillis() {
        return expiryDays * 24L * 60L * 60L * 1000L;
    }

    public int maxQuantity() {
        return maxQuantity;
    }

    public int collectCapacity() {
        return collectCapacity;
    }

    public int promptTicks() {
        return (int) promptTicks;
    }

    public boolean chestEnabled() {
        return chestEnabled;
    }

    public boolean logEnabled() {
        return logEnabled;
    }

    public long rotateBytes() {
        return rotateBytes;
    }

    public int logLines() {
        return logLines;
    }

    public double fairnessRatio() {
        return fairnessRatio;
    }

    /** Ordered category tab names from config.yml. */
    public List<String> categoryNames() {
        return new ArrayList<>(categoryNames);
    }

    /** Category for an item: custom items first, then the configured material mapping. */
    public String category(ItemStack item) {
        if (item == null) {
            return "Misc";
        }
        boolean custom = item.hasItemMeta()
                && (item.getItemMeta().displayName() != null
                || !item.getItemMeta().lore().isEmpty()
                || item.getItemMeta().hasCustomModelData());
        if (custom && categoryNames.contains("Custom items")) {
            return "Custom items";
        }
        String category = categories.get(item.getType());
        return category == null ? "Misc" : category;
    }

    /** Air, non-items and spawn eggs are always rejected; server shops skip the lists. */
    public boolean paymentAllowed(Material material, boolean serverShop) {
        if (material == null || material.isAir() || !material.isItem()
                || material.name().endsWith("_SPAWN_EGG")) {
            return false;
        }
        if (serverShop) {
            return true;
        }
        if (whitelistEnabled) {
            return whitelist.contains(normalize(material.name()));
        }
        return !blacklist.contains(normalize(material.name()));
    }

    public double value(Material material) {
        return values.getOrDefault(material, 0.0);
    }

    /** Suggested payment stack size for one purchased unit. 0 means no hint. */
    public int suggested(ItemStack sold, ItemStack payment) {
        double sellValue = value(sold.getType());
        double payValue = value(payment.getType());
        if (sellValue <= 0 || payValue <= 0) {
            return 0;
        }
        long suggestion = (long) Math.ceil(sellValue / payValue);
        return suggestion > 0 && suggestion <= 9999 ? (int) suggestion : 0;
    }

    /** Fairness is a warning only, never a block. */
    public boolean unfair(int price, ItemStack sold, ItemStack payment) {
        int suggestion = suggested(sold, payment);
        if (fairnessRatio <= 0 || suggestion <= 0 || price <= 0) {
            return false;
        }
        double ratio = (double) price / suggestion;
        return ratio > fairnessRatio || ratio < 1.0 / fairnessRatio;
    }
}
