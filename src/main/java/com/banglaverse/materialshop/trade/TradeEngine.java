package com.banglaverse.materialshop.trade;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.CollectBox;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.PaymentOption;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Main-thread atomic trade engine with snapshot rollback and per-listing locks. */
public final class TradeEngine {
    private final MaterialShop plugin;
    private final Set<String> locked = ConcurrentHashMap.newKeySet();

    public TradeEngine(MaterialShop plugin) {
        this.plugin = plugin;
    }

    private int stock(Listing listing) {
        if (listing.source() == Listing.Source.SERVER) {
            return plugin.settings().maxQuantity();
        }
        if (listing.source() == Listing.Source.CHEST) {
            return plugin.chests() == null ? 0 : plugin.chests().stock(listing);
        }
        return listing.stock();
    }

    /** Largest quantity possible right now. Never changes state. */
    public int maxQuantity(Player buyer, Listing listing, int optionIndex) {
        if (buyer == null || !buyer.isOnline() || listing == null || listing.expired()
                || listing.owner() != null && listing.owner().equals(buyer.getUniqueId())) {
            return 0;
        }
        PaymentOption option = listing.option(optionIndex);
        if (option == null || !plugin.settings().paymentAllowed(option.item().getType(),
                listing.source() == Listing.Source.SERVER)) {
            return 0;
        }
        int payment = Items.count(buyer.getInventory(), option.item());
        int affordable = payment / Math.max(1, option.amount());
        int room = Items.room(buyer.getInventory(), listing.item());
        return Math.max(0, Math.min(plugin.settings().maxQuantity(),
                Math.min(stock(listing), Math.min(affordable, room))));
    }

    /** Validates, snapshots, transfers and logs one purchase. No partial trades. */
    public boolean execute(Player buyer, Listing listing, int optionIndex, int quantity) {
        String key = String.valueOf(listing.id());
        if (!locked.add(key)) {
            plugin.msg().send(buyer, "trade-failed");
            return false;
        }
        try {
            return executeLocked(buyer, listing, optionIndex, quantity);
        } finally {
            locked.remove(key);
        }
    }

    private boolean executeLocked(Player buyer, Listing listing, int optionIndex, int quantity) {
        if (buyer == null || !buyer.isOnline() || listing == null || listing.expired()) {
            plugin.msg().send(buyer, "listing-gone");
            return false;
        }
        if (listing.owner() != null && listing.owner().equals(buyer.getUniqueId())) {
            plugin.msg().send(buyer, "self-buy");
            return false;
        }
        PaymentOption option = listing.option(optionIndex);
        if (option == null || quantity < 1) {
            plugin.msg().send(buyer, "trade-failed");
            return false;
        }
        if (quantity > maxQuantity(buyer, listing, optionIndex)) {
            if (stock(listing) < quantity) {
                plugin.msg().send(buyer, "not-enough-stock", "item", Items.name(listing.item()));
            } else {
                plugin.msg().send(buyer, "not-enough-payment", "item", Items.name(option.item()));
            }
            return false;
        }
        int needed = option.amount() * quantity;
        boolean collectNeeded = listing.source() != Listing.Source.SERVER;
        CollectBox box = collectNeeded ? plugin.data().box(listing.owner()) : null;
        if (collectNeeded && !box.canHold(option.item(), needed, plugin.settings().collectCapacity())) {
            plugin.msg().send(buyer, "collect-full");
            return false;
        }

        // Snapshot every mutable thing before touching the buyer.
        ItemStack[] buyerBefore = Items.cloneContents(buyer.getInventory().getStorageContents());
        List<ItemStack> escrowBefore = listing.escrow();
        List<ItemStack> collectBefore = box == null ? null : box.stacks();
        List<ItemStack> chestBefore = null;
        if (listing.source() == Listing.Source.CHEST && plugin.chests() != null) {
            chestBefore = plugin.chests().withdraw(listing, quantity);
            if (chestBefore.size() != quantity) {
                plugin.chests().deposit(listing, chestBefore);
                plugin.msg().send(buyer, "not-enough-stock", "item", Items.name(listing.item()));
                return false;
            }
        }

        boolean ok = false;
        try {
            // Order: remove payment, check room, give items, remove escrow, store payment.
            int removed = Items.remove(buyer.getInventory(), option.item(), needed);
            if (removed != needed) {
                plugin.msg().send(buyer, "not-enough-payment", "item", Items.name(option.item()));
                return false;
            }
            if (Items.room(buyer.getInventory(), listing.item()) < quantity) {
                plugin.msg().send(buyer, "no-room", "item", Items.name(listing.item()));
                return false;
            }
            Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(
                    Items.stacks(listing.item(), quantity).toArray(new ItemStack[0]));
            if (!leftovers.isEmpty()) {
                plugin.msg().send(buyer, "no-room", "item", Items.name(listing.item()));
                return false;
            }
            if (listing.source() == Listing.Source.PLAYER) {
                for (int i = 0; i < quantity; i++) {
                    if (listing.takeUnit() == null) {
                        plugin.msg().send(buyer, "not-enough-stock", "item", Items.name(listing.item()));
                        return false;
                    }
                }
            }
            if (collectNeeded && !box.add(option.item(), needed, plugin.settings().collectCapacity()).isEmpty()) {
                plugin.msg().send(buyer, "collect-full");
                return false;
            }
            plugin.data().save();
            plugin.logs().log(buyer.getName(), sellerName(listing),
                    Items.describe(listing.item(), quantity), Items.describe(option.item(), needed),
                    listing.id().toString());
            plugin.msg().send(buyer, "trade-success", "item", Items.describe(listing.item(), quantity),
                    "payment", Items.describe(option.item(), needed));
            ok = true;
            return true;
        } finally {
            if (!ok) {
                buyer.getInventory().setStorageContents(buyerBefore);
                listing.escrow(escrowBefore);
                if (box != null) {
                    box.stacks(collectBefore);
                }
                if (chestBefore != null && plugin.chests() != null) {
                    plugin.chests().deposit(listing, chestBefore);
                }
                plugin.data().save();
            }
        }
    }

    private String sellerName(Listing listing) {
        if (listing.source() == Listing.Source.SERVER) {
            return "server";
        }
        var offline = Bukkit.getOfflinePlayer(listing.owner());
        return offline.getName() == null ? String.valueOf(listing.owner()) : offline.getName();
    }
}
