package com.banglaverse.materialshop.listener;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.menu.ShopViewMenu;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/** Tagged villagers open their server shop instead of their trading screen. */
public final class NpcListener implements Listener {
    private final MaterialShop plugin;

    public NpcListener(MaterialShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        String raw = villager.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "shop"), PersistentDataType.STRING);
        if (raw == null) {
            return;
        }
        try {
            Shop shop = plugin.data().shop(java.util.UUID.fromString(raw));
            if (shop == null || !shop.isServer()) {
                return;
            }
            event.setCancelled(true);
            new ShopViewMenu(plugin, event.getPlayer(), shop.id(), false).open();
        } catch (IllegalArgumentException ignored) {
            // Stale tag: behave like a normal villager.
        }
    }
}
