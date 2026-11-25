package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Listing;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Paginated marketplace with category tabs and newest/name sorting. */
public final class MarketMenu extends MenuBase {
    private static final int[] SLOTS = {
            18, 19, 20, 21, 22, 23, 24, 25,
            27, 28, 29, 30, 31, 32, 33, 34,
            36, 37, 38, 39, 40, 41, 42, 43
    };

    private final int category;
    private final boolean sortByName;
    private final int page;

    public MarketMenu(MaterialShop plugin, Player player) {
        this(plugin, player, 0, false, 0);
    }

    public MarketMenu(MaterialShop plugin, Player player, int category, boolean sortByName, int page) {
        super(plugin, player);
        this.category = category;
        this.sortByName = sortByName;
        this.page = page;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String titleKey() {
        return "menu.market.title";
    }

    @Override
    protected String[] titleArgs() {
        List<Listing> shown = shown();
        return new String[]{"page", String.valueOf(page + 1),
                "pages", String.valueOf(Math.max(1, (shown.size() + SLOTS.length - 1) / SLOTS.length))};
    }

    private List<Listing> shown() {
        List<String> categories = plugin.settings().categoryNames();
        String wanted = category <= 0 || category > categories.size() ? null : categories.get(category - 1);
        List<Listing> out = new ArrayList<>();
        for (Listing listing : plugin.data().listings().values()) {
            if (listing.source() != Listing.Source.PLAYER || listing.expired() || listing.stock() <= 0) {
                continue;
            }
            if (wanted != null && !plugin.settings().category(listing.item()).equals(wanted)) {
                continue;
            }
            out.add(listing);
        }
        if (sortByName) {
            out.sort(Comparator.comparing(listing -> Items.name(listing.item())));
        } else {
            out.sort(Comparator.comparingLong(Listing::created).reversed());
        }
        return out;
    }

    @Override
    protected void build() {
        List<String> categories = plugin.settings().categoryNames();
        for (int i = 0; i <= Math.min(17, categories.size()); i++) {
            int index = i;
            String label = i == 0 ? plugin.msg().string("menu.market.all", "All") : categories.get(i - 1);
            ItemStack tab = new ItemStack(index == category ? Material.GOLD_BLOCK : Material.STONE);
            ItemMeta meta = tab.getItemMeta();
            meta.displayName(Component.text(label));
            tab.setItemMeta(meta);
            int slot = i < 9 ? 1 + i : 10 + (i - 9);
            set(slot, tab, () -> new MarketMenu(plugin, player, index, sortByName, 0).open());
        }
        set(0, icon(Material.ARROW, sortByName ? "menu.market.sort-name" : "menu.market.sort-newest"),
                () -> new MarketMenu(plugin, player, category, !sortByName, 0).open());

        List<Listing> listings = shown();
        int pages = Math.max(1, (listings.size() + SLOTS.length - 1) / SLOTS.length);
        int current = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < SLOTS.length && current * SLOTS.length + i < listings.size(); i++) {
            Listing listing = listings.get(current * SLOTS.length + i);
            String price = listing.options().isEmpty() ? "-" : Items.describe(
                    listing.options().get(0).item(), listing.options().get(0).amount());
            ItemStack display = itemIcon(listing.item(), "menu.market.listing",
                    line("menu.market.listing-lore", "stock", String.valueOf(listing.stock()),
                            "price", plugin.msg().clean(price)));
            set(SLOTS[i], display, () -> new PurchaseMenu(plugin, player, listing.id()).open());
        }
        if (listings.isEmpty()) {
            set(31, icon(Material.BARRIER, "menu.market.empty"));
        }
        set(48, icon(Material.ARROW, "menu.common.previous"),
                () -> new MarketMenu(plugin, player, category, sortByName, current - 1).open());
        set(49, icon(Material.PAPER, "menu.common.page", "page", String.valueOf(current + 1),
                "pages", String.valueOf(pages)));
        set(50, icon(Material.ARROW, "menu.common.next"),
                () -> new MarketMenu(plugin, player, category, sortByName, current + 1).open());
        backToMain(53);
    }
}
