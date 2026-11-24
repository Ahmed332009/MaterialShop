package dev.materialshop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Optional MaterialClaim/WorldGuard checks through reflection: no compile-time dependency
 * and no failure at all when either plugin is absent.
 */
public final class TerritoryHook {
    private final MaterialShop plugin;
    private boolean warned;

    public TerritoryHook(MaterialShop plugin) {
        this.plugin = plugin;
    }

    /** True when a player may place a shop chest at this block. */
    public boolean canCreate(Player player, Block block) {
        if (player.hasPermission("materialshop.admin.bypass")) {
            return true;
        }
        return materialClaim(player, block) && worldGuard(player, block);
    }

    private boolean materialClaim(Player player, Block block) {
        if (!plugin.settings().claimsHook()) {
            return true;
        }
        Plugin claimPlugin = plugin.getServer().getPluginManager().getPlugin("MaterialClaim_Block");
        if (claimPlugin == null) {
            claimPlugin = plugin.getServer().getPluginManager().getPlugin("MaterialClaim_Chunk");
        }
        if (claimPlugin == null) {
            return true;
        }
        try {
            Object claims = claimPlugin.getClass().getMethod("claims").invoke(claimPlugin);
            Object claim = claims.getClass().getMethod("at", Location.class).invoke(claims, block.getLocation());
            if (claim == null) {
                return true;
            }
            Object settings = claimPlugin.getClass().getMethod("settings").invoke(claimPlugin);
            return (Boolean) settings.getClass()
                    .getMethod("canManage", claim.getClass())
                    .invoke(settings, player, claim);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            warn("MaterialClaim hook: " + ex.getMessage());
            return true; // A soft dependency must never break shop creation.
        }
    }

    private boolean worldGuard(Player player, Block block) {
        if (!plugin.settings().worldGuardHook()) {
            return true;
        }
        try {
            Class<?> type = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Object instance = type.getMethod("inst").invoke(null);
            return (Boolean) type.getMethod("canBuild", Player.class, Block.class)
                    .invoke(instance, player, block);
        } catch (ClassNotFoundException ignored) {
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            warn("WorldGuard hook: " + ex.getMessage());
            return true;
        }
    }

    private void warn(String message) {
        if (!warned) {
            warned = true;
            plugin.getLogger().warning("Soft-dependency check failed, allowing the shop: " + message);
        }
    }
}
