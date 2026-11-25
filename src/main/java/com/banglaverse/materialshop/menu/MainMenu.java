package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.MaterialShop;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** The central /shop menu. */
public final class MainMenu extends MenuBase {
    public MainMenu(MaterialShop plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String titleKey() {
        return "menu.main.title";
    }

    @Override
    protected void build() {
        set(10, icon(Material.GOLD_INGOT, "menu.main.market"),
                () -> new MarketMenu(plugin, player).open());
        set(12, icon(Material.BRICKS, "menu.main.server"),
                () -> new ServerShopMenu(plugin, player).open());
        set(14, icon(Material.BOOK, "menu.main.mine", "count",
                String.valueOf(plugin.data().listingsByOwner(player.getUniqueId()).size())),
                () -> new MyListingsMenu(plugin, player).open());
        set(16, icon(Material.HOPPER, "menu.main.collect"),
                () -> new CollectMenu(plugin, player).open());
        if (plugin.settings().chestEnabled()) {
            set(20, icon(Material.CHEST, "menu.main.chest"),
                    () -> player.performCommand("shop chest create"));
            set(22, icon(Material.BARREL, "menu.main.chest-list"),
                    () -> new ChestShopListMenu(plugin, player).open());
        }
        if (player.hasPermission("materialshop.admin")) {
            set(24, icon(Material.CROSSBOW, "menu.main.admin"),
                    () -> player.performCommand("shopadmin"));
        }
        set(26, icon(Material.BARRIER, "menu.common.close"), player::closeInventory);
    }
}
