package dev.materialshop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Linked villagers / NPCs act as shop opening points; they are not trade containers. */
public final class ShopEntityListener implements Listener {
    private final MaterialShop plugin;

    public ShopEntityListener(MaterialShop plugin) {
        this.plugin = plugin;
    }

    private Shop linked(org.bukkit.entity.Entity entity) {
        for (Shop shop : plugin.shops().all()) {
            if (entity.getUniqueId().equals(shop.entityUuid())) {
                return shop;
            }
        }
        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Shop shop = linked(event.getRightClicked());
        if (shop == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer() instanceof Player player) {
            if (shop.open()) {
                plugin.gui().openShop(player, shop, 0);
            } else {
                plugin.msg().send(player, "shop-closed", "name", plugin.msg().escape(shop.name()));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Shop shop = linked(event.getEntity());
        if (shop == null || shop.isAdmin()) {
            return;
        }
        if (event.getDamager() instanceof Player player && plugin.shops().mayManage(player, shop)) {
            return;
        }
        event.setCancelled(true);
    }
}
