package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.PaymentOption;
import com.banglaverse.materialshop.trade.Rewards;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stock, price and cancel controls for one of the seller's listings. */
public final class ListingManageMenu extends MenuBase {
    private final UUID listingId;

    public ListingManageMenu(MaterialShop plugin, Player player, UUID listingId) {
        super(plugin, player);
        this.listingId = listingId;
    }

    private Listing fresh() {
        Listing listing = plugin.data().listing(listingId);
        if (listing == null) {
            plugin.msg().send(player, "listing-gone");
            new MyListingsMenu(plugin, player).open();
            return null;
        }
        return listing;
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String titleKey() {
        return "menu.manage.title";
    }

    @Override
    protected void build() {
        Listing listing = plugin.data().listing(listingId);
        if (listing == null) {
            return;
        }
        set(4, itemIcon(listing.item(), "menu.manage.item-line", List.of(
                plugin.msg().plain("menu.manage.stock-line", "stock", String.valueOf(listing.stock())))));
        int index = 0;
        for (PaymentOption option : listing.options()) {
            int slot = 18 + index;
            int optionIndex = index++;
            set(slot, itemIcon(option.item(), "menu.manage.payment-line",
                    List.of(plugin.msg().plain("menu.manage.change-price-lore")),
                    "index", String.valueOf(optionIndex + 1)), () -> changePrice(optionIndex));
        }
        set(10, icon(Material.CHEST, "menu.manage.add-stock"), this::addStock);
        set(16, icon(Material.BARRIER, "menu.manage.cancel"), this::cancelListing);
        backButton(26, new MainMenu(plugin, player));
    }

    private void addStock() {
        Listing listing = fresh();
        if (listing == null) {
            return;
        }
        int perUnit = listing.item().getAmount();
        int available = Items.count(player.getInventory(), listing.item()) / Math.max(1, perUnit);
        if (available <= 0) {
            plugin.msg().send(player, "not-enough-stock", "item", Items.name(listing.item()));
            return;
        }
        plugin.msg().send(player, "stock-prompt");
        plugin.prompts().request(player, text -> {
            int units;
            try {
                units = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", String.valueOf(available));
                return;
            }
            if (units < 1 || units > available) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", String.valueOf(available));
                return;
            }
            int removed = Items.remove(player.getInventory(), listing.item(), units * perUnit);
            if (removed != units * perUnit) {
                plugin.msg().send(player, "trade-failed");
                return;
            }
            for (int i = 0; i < units; i++) {
                listing.addUnit(listing.item());
            }
            plugin.data().saveListing(listing);
            plugin.msg().send(player, "escrow-added", "amount", String.valueOf(units));
            new ListingManageMenu(plugin, player, listingId).open();
        });
    }

    private void changePrice(int optionIndex) {
        Listing listing = fresh();
        if (listing == null || optionIndex >= listing.options().size()) {
            return;
        }
        PaymentOption option = listing.options().get(optionIndex);
        plugin.msg().send(player, "price-prompt", "item", Items.name(option.item()));
        plugin.prompts().request(player, text -> {
            int amount;
            try {
                amount = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", "64");
                return;
            }
            if (amount < 1 || amount > 64) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", "64");
                return;
            }
            List<PaymentOption> options = new ArrayList<>(listing.options());
            PaymentOption replacement = new PaymentOption(options.get(optionIndex).item());
            replacement.amount(amount);
            options.set(optionIndex, replacement);
            listing.options(options);
            plugin.data().saveListing(listing);
            plugin.msg().send(player, "listing-updated");
            new ListingManageMenu(plugin, player, listingId).open();
        });
    }

    private void cancelListing() {
        Listing listing = fresh();
        if (listing == null) {
            return;
        }
        if (listing.source() == Listing.Source.CHEST) {
            plugin.chests().close(plugin.data().shop(listing.shopId()));
            plugin.msg().send(player, "chest-closed");
            new com.banglaverse.materialshop.menu.ChestShopListMenu(plugin, player).open();
            return;
        }
        Rewards.give(plugin, listing.owner(), listing.escrow());
        plugin.data().removeListing(listingId);
        plugin.msg().send(player, "listing-cancelled");
        new MyListingsMenu(plugin, player).open();
    }
}
