package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Listings inside one server shop or chest shop. */
public final class ShopViewMenu extends MenuBase {
    private final UUID shopId;
    private final boolean admin;

    public ShopViewMenu(MaterialShop plugin, Player player, UUID shopId, boolean admin) {
        super(plugin, player);
        this.shopId = shopId;
        this.admin = admin;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String titleKey() {
        return "menu.shop.title";
    }

    @Override
    protected String[] titleArgs() {
        Shop shop = plugin.data().shop(shopId);
        return new String[]{"name", plugin.msg().clean(shop == null ? "Shop" : shop.name())};
    }

    @Override
    protected void build() {
        Shop shop = plugin.data().shop(shopId);
        if (shop == null) {
            plugin.msg().send(player, "server-shop-not-found");
            backToMain(53);
            return;
        }
        int slot = 10;
        for (Listing listing : plugin.data().listingsByShop(shopId)) {
            if (slot > 43) {
                break;
            }
            String price = listing.options().isEmpty() ? "-" : Items.describe(
                    listing.options().get(0).item(), listing.options().get(0).amount());
            ItemStack display = itemIcon(listing.item(), "menu.market.listing",
                    line("menu.market.listing-lore", "stock",
                            shop.isServer() ? plugin.msg().string("word-infinite", "infinite")
                                    : String.valueOf(plugin.chests() == null ? 0 : plugin.chests().stock(listing)),
                            "price", plugin.msg().clean(price)));
            UUID listingId = listing.id();
            set(slot++, display, () -> {
                if (admin) {
                    new SetupMenu(plugin, player, SetupMenu.Mode.SERVER, shopId, null, shop.name(), listingId).open();
                } else {
                    new PurchaseMenu(plugin, player, listingId).open();
                }
            });
        }
        if (plugin.data().listingsByShop(shopId).isEmpty()) {
            set(22, icon(Material.BARRIER, "menu.shop.empty"));
        }
        if (admin) {
            set(0, icon(Material.EMERALD, "menu.admin.add-listing"),
                    () -> new SetupMenu(plugin, player, SetupMenu.Mode.SERVER, shopId, null, shop.name(), null).open());
        }
        if (shop.isServer()) {
            set(53, icon(Material.BARRIER, "menu.common.back"),
                    () -> new ServerShopMenu(plugin, player).open());
        } else {
            backToMain(53);
        }
    }
}
