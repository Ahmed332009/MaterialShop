package dev.materialshop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Config-backed rules: shop limits, payment filtering, values, logging, soft deps. */
public final class SettingsService {
    private final MaterialShop plugin;

    private int shopLimit = 5;
    private int collectRows = 6;
    private int promptTicks = 600;
    private boolean useWhitelist = false;
    private boolean adminOverride = true;
    private final Set<String> blacklist = new HashSet<>();
    private final Set<String> whitelist = new HashSet<>();
    private final Map<Material, Double> values = new HashMap<>();
    private double warnRatio = 4.0;
    private boolean logEnabled = true;
    private long rotateBytes = 5L * 1024L * 1024L;
    private int logLines = 12;
    private boolean claimsHook = true;
    private boolean worldGuardHook = true;

    public SettingsService(MaterialShop plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        YamlConfiguration cfg = plugin.getConfig();
        shopLimit = Math.max(1, cfg.getInt("shop-limit", 5));
        collectRows = Math.max(1, Math.min(6, cfg.getInt("collect-box-rows", 6)));
        promptTicks = Math.max(5, cfg.getInt("chat-timeout-seconds", 30)) * 20;
        useWhitelist = cfg.getBoolean("payment.use-whitelist", false);
        adminOverride = cfg.getBoolean("payment.admin-allowed-override", true);
        blacklist.clear();
        whitelist.clear();
        for (String s : cfg.getStringList("payment.blacklist")) {
            blacklist.add(normalize(s));
        }
        for (String s : cfg.getStringList("payment.whitelist")) {
            whitelist.add(normalize(s));
        }
        values.clear();
        ConfigurationSection valueSec = cfg.getConfigurationSection("material-values");
        if (valueSec != null) {
            for (String key : valueSec.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                double value = valueSec.getDouble(key, 0.0);
                if (m != null && value > 0) {
                    values.put(m, value);
                } else if (m == null) {
                    plugin.getLogger().warning("Unknown material in material-values: " + key);
                }
            }
        }
        warnRatio = Math.max(0.0, cfg.getDouble("fairness.warn-ratio", 4.0));
        logEnabled = cfg.getBoolean("logging.enabled", true);
        rotateBytes = Math.max(64L, cfg.getLong("logging.rotate-megabytes", 5L)) * 1024L * 1024L;
        logLines = Math.max(1, cfg.getInt("logging.log-lines-in-gui", 12));
        claimsHook = cfg.getBoolean("soft-dependencies.materialclaim", true);
        worldGuardHook = cfg.getBoolean("soft-dependencies.worldguard", true);
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('.', '_');
    }

    public int defaultShopLimit() {
        return shopLimit;
    }

    public int collectRows() {
        return collectRows;
    }

    public int collectSlots() {
        return collectRows * 9;
    }

    public int promptTicks() {
        return promptTicks;
    }

    public int logLines() {
        return logLines;
    }

    public boolean logEnabled() {
        return logEnabled;
    }

    public long rotateBytes() {
        return rotateBytes;
    }

    public boolean claimsHook() {
        return claimsHook;
    }

    public boolean worldGuardHook() {
        return worldGuardHook;
    }

    /** Highest materialshop.limit.&lt;n&gt; permission, never below the config default. */
    public int shopLimit(Player player) {
        int limit = shopLimit;
        if (player.hasPermission("materialshop.admin.bypass")) {
            return Integer.MAX_VALUE;
        }
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String permission = info.getPermission();
            String prefix = "materialshop.limit.";
            if (permission.startsWith(prefix)) {
                try {
                    limit = Math.max(limit, Integer.parseInt(permission.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // Not a numeric limit, e.g. the wildcard parent node.
                }
            }
        }
        return limit;
    }

    /** Material-level global rule. Wildcards like _SPAWN_EGG are supported. */
    public boolean globalPaymentAllowed(Material material) {
        if (material == null || material.isAir()) {
            return false;
        }
        String name = material.name();
        for (String banned : blacklist) {
            if (banned.startsWith("_") ? name.endsWith(banned) : name.equals(banned)) {
                return false;
            }
        }
        if (useWhitelist) {
            for (String allowed : whitelist) {
                if (allowed.startsWith("_") ? name.endsWith(allowed) : name.equals(allowed)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Full payment rule for one shop. Admin shops may add a per-shop allow/deny
     * override; player shops always use the global rule.
     */
    public boolean paymentAllowed(Material material, Shop shop) {
        if (!globalPaymentAllowed(material)) {
            return false;
        }
        if (shop == null || !shop.isAdmin() || !adminOverride) {
            return true;
        }
        String mode = shop.paymentOverride();
        if ("ALLOW".equalsIgnoreCase(mode)) {
            return shop.paymentOverrideList().contains(normalize(material.name()));
        }
        if ("DENY".equalsIgnoreCase(mode)) {
            return !shop.paymentOverrideList().contains(normalize(material.name()));
        }
        return true;
    }

    /** Point value of a material. Named/enchanted/custom items use their material value. */
    public double valueOf(Material material) {
        return values.getOrDefault(material, 0.0);
    }

    /** Suggested number of payment items for one sold item, rounded up. 0 = no hint. */
    public int suggestedAmount(ItemStack sold, ItemStack payment) {
        double sellValue = valueOf(sold.getType());
        double payValue = valueOf(payment.getType());
        if (sellValue <= 0 || payValue <= 0) {
            return 0;
        }
        long suggestion = (long) Math.ceil(sellValue / payValue);
        return suggestion > 0 && suggestion <= 9999 ? (int) suggestion : 0;
    }

    /** True when a price is lopsided. Only ever used for a warning message. */
    public boolean unfair(int price, ItemStack sold, ItemStack payment) {
        if (warnRatio <= 0 || price <= 0) {
            return false;
        }
        int suggested = suggestedAmount(sold, payment);
        if (suggested <= 0) {
            return false;
        }
        double ratio = (double) price / suggested;
        return ratio > warnRatio || ratio < 1.0 / warnRatio;
    }

    public List<String> valueKeys() {
        return new ArrayList<>(values.keySet().stream().map(m -> m.name()).toList());
    }
}
