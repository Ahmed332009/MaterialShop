package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Listing;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** The seller's own listings and the new-listing button. */
public final class MyListingsMenu extends MenuBase {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final int page;

    public MyListingsMenu(MaterialShop plugin, Player player) {
        this(plugin, player, 0);
    }

    public MyListingsMenu(MaterialShop plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String titleKey() {
        return "menu.mine.title";
    }

    @Override
    protected String[] titleArgs() {
        List<Listing> own = plugin.data().listingsByOwner(player.getUniqueId());
        return new String[]{"page", String.valueOf(page + 1),
                "pages", String.valueOf(Math.max(1, (own.size() + SLOTS.length - 1) / SLOTS.length))};
    }

    @Override
    protected void build() {
        set(0, icon(Material.EMERALD, "menu.mine.new"),
                () -> new SetupMenu(plugin, player).open());
        List<Listing> own = new ArrayList<>(plugin.data().listingsByOwner(player.getUniqueId()));
        int pages = Math.max(1, (own.size() + SLOTS.length - 1) / SLOTS.length);
        int current = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < SLOTS.length && current * SLOTS.length + i < own.size(); i++) {
            Listing listing = own.get(current * SLOTS.length + i);
            set(SLOTS[i], itemIcon(listing.item(), "menu.mine.listing",
                    line("menu.mine.listing-lore", "stock", String.valueOf(listing.stock()))),
                    () -> new ListingManageMenu(plugin, player, listing.id()).open());
        }
        if (own.isEmpty()) {
            set(31, icon(Material.BARRIER, "menu.mine.empty"));
        }
        set(48, icon(Material.ARROW, "menu.common.previous"),
                () -> new MyListingsMenu(plugin, player, current - 1).open());
        set(49, icon(Material.PAPER, "menu.common.page", "page", String.valueOf(current + 1),
                "pages", String.valueOf(pages)));
        set(50, icon(Material.ARROW, "menu.common.next"),
                () -> new MyListingsMenu(plugin, player, current + 1).open());
        backToMain(53);
    }
}
