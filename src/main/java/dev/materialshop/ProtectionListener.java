package dev.materialshop;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

/** Protects player-shop chests from everyone except their owner (and admins). */
public final class ProtectionListener implements Listener {
    private final MaterialShop plugin;

    public ProtectionListener(MaterialShop plugin) {
        this.plugin = plugin;
    }

    private Shop shopAt(Block block) {
        if (block == null) {
            return null;
        }
        for (Shop shop : plugin.shops().all()) {
            if (shop.isAdmin() || shop.location() == null) {
                continue;
            }
            if (shop.location().getWorld().equals(block.getWorld())
                    && shop.location().getBlockX() == block.getX()
                    && shop.location().getBlockY() == block.getY()
                    && shop.location().getBlockZ() == block.getZ()) {
                return shop;
            }
        }
        return null;
    }

    private boolean mayUse(Player player, Shop shop) {
        return player.hasPermission("materialshop.admin.bypass") || shop.ownedBy(player.getUniqueId());
    }

    private void deny(Player player, Shop shop) {
        plugin.msg().send(player, "chest-protected", "owner", plugin.shops().ownerName(shop));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Shop shop = shopAt(event.getBlock());
        if (shop != null && !mayUse(event.getPlayer(), shop)) {
            event.setCancelled(true);
            deny(event.getPlayer(), shop);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Shop shop = shopAt(event.getClickedBlock());
        if (shop != null && !mayUse(event.getPlayer(), shop)) {
            event.setCancelled(true);
            deny(event.getPlayer(), shop);
        }
    }

    /** Hoppers (and every other inventory-moving block) may never touch a shop chest. */
    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        if (isShopChest(event.getSource()) || isShopChest(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isShopChest(InventoryHolder holder) {
        return holder instanceof Chest chest && shopAt(chest.getBlock()) != null;
    }

    @EventHandler
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Shop shop = shopAt(block);
            if (shop != null) {
                plugin.getLogger().info("Explosion blocked at shop chest of " + shop.id());
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> shopAt(block) != null);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (shopAt(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (shopAt(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
