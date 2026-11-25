package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** List of the player's physical chest shops. */
public final class ChestShopListMenu extends MenuBase {
    public ChestShopListMenu(MaterialShop plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String titleKey() {
        return "menu.chest.list-title";
    }

    @Override
    protected void build() {
        List<Shop> shops = plugin.data().shops(Shop.Type.CHEST).stream()
                .filter(shop -> shop.ownedBy(player.getUniqueId())).toList();
        int slot = 10;
        for (Shop shop : shops) {
            if (slot > 25) {
                break;
            }
            String x = shop.location() == null ? "?" : String.valueOf(shop.location().getBlockX());
            String z = shop.location() == null ? "?" : String.valueOf(shop.location().getBlockZ());
            set(slot++, itemIcon(new ItemStack(Material.CHEST), "menu.chest.entry",
                    line("menu.chest.entry-lore", "count",
                            String.valueOf(plugin.data().listingsByShop(shop.id()).size()),
                            "x", x, "z", z),
                    "name", plugin.msg().clean(shop.name())),
                    () -> new ShopViewMenu(plugin, player, shop.id(), false).open());
        }
        if (shops.isEmpty()) {
            set(13, icon(Material.BARRIER, "menu.chest.empty"));
        }
        backToMain(26);
    }
}
