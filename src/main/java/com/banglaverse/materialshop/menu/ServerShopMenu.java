package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** List of admin-created server shops with unlimited stock. */
public final class ServerShopMenu extends MenuBase {
    public ServerShopMenu(MaterialShop plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String titleKey() {
        return "menu.shop.title";
    }

    @Override
    protected String[] titleArgs() {
        return new String[]{"name", plugin.msg().string("menu.main.server", "Server shop")};
    }

    @Override
    protected void build() {
        List<Shop> shops = plugin.data().shops(Shop.Type.SERVER);
        int slot = 10;
        for (Shop shop : shops) {
            if (slot > 25) {
                break;
            }
            set(slot++, itemIcon(new ItemStack(Material.BRICKS), "menu.shop.entry",
                    line("menu.shop.entry-lore", "count",
                            String.valueOf(plugin.data().listingsByShop(shop.id()).size())),
                    "name", plugin.msg().clean(shop.name())),
                    () -> new ShopViewMenu(plugin, player, shop.id(), false).open());
        }
        if (shops.isEmpty()) {
            set(13, icon(Material.BARRIER, "menu.shop.empty"));
        }
        backToMain(26);
    }
}
