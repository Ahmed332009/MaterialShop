package com.banglaverse.materialshop.chest;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.menu.ShopViewMenu;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** Protects shop containers and opens their GUI for everyone but the owner. */
public final class ChestShopListener implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final MaterialShop plugin;

    public ChestShopListener(MaterialShop plugin) {
        this.plugin = plugin;
    }

    private boolean allowed(Player player, Shop shop) {
        return player.hasPermission("materialshop.admin.bypass") || shop.ownedBy(player.getUniqueId());
    }

    private void deny(Player player, Shop shop) {
        plugin.msg().send(player, "chest-protected", "owner",
                plugin.msg().clean(String.valueOf(shop.owner())));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Shop shop = plugin.chests().shopAt(event.getBlock());
        if (shop != null && !allowed(event.getPlayer(), shop)) {
            event.setCancelled(true);
            deny(event.getPlayer(), shop);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Shop shop = plugin.chests().shopAt(event.getClickedBlock());
        if (shop == null || allowed(event.getPlayer(), shop)) {
            return;
        }
        event.setCancelled(true);
        new ShopViewMenu(plugin, event.getPlayer(), shop.id(), false).open();
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        if (isShopContainer(event.getSource().getHolder()) || isShopContainer(event.getDestination().getHolder())) {
            event.setCancelled(true);
        }
    }

    private boolean isShopContainer(org.bukkit.inventory.InventoryHolder holder) {
        return holder instanceof Container container && plugin.chests().shopAt(container.getBlock()) != null;
    }

    @EventHandler
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.chests().shopAt(block) != null);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.chests().shopAt(block) != null);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.chests().shopAt(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.chests().shopAt(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Prevents a neighbouring chest from forming a double chest with a shop container. */
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!(event.getBlock().getState() instanceof Container)) {
            return;
        }
        for (BlockFace face : FACES) {
            Block neighbour = event.getBlock().getRelative(face);
            if (plugin.chests().shopAt(neighbour) != null) {
                event.setCancelled(true);
                plugin.msg().send(event.getPlayer(), "chest-protected", "owner", "a shop");
                return;
            }
        }
    }
}
