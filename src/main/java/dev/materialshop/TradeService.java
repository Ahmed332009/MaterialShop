package dev.materialshop;

import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atomic barter engine. The whole trade happens in one main-thread click: verify,
 * withdraw, remove stock, give items and credit the collect box, with a full rollback
 * on any failure. A per-listing lock protects against simultaneous buyers.
 */
public final class TradeService {
    /** 36 stacks of 64: more than one vanilla inventory can hold. */
    public static final int HARD_CAP = 2304;

    private final MaterialShop plugin;
    private final Set<String> locked = ConcurrentHashMap.newKeySet();

    public TradeService(MaterialShop plugin) {
        this.plugin = plugin;
    }

    public boolean isLocked(Shop shop, Listing listing) {
        return locked.contains(shop.id() + ":" + listing.id());
    }

    /** Largest quantity the buyer could complete right now. Never mutates anything. */
    public int maxQuantity(Player buyer, Shop shop, Listing listing, int optionIndex) {
        if (buyer == null || shop == null || !shop.open() || listing == null || !buyer.isOnline()) {
            return 0;
        }
        if (shop.ownedBy(buyer.getUniqueId())) {
            return 0;
        }
        PaymentOption option = listing.option(optionIndex);
        if (option == null || !plugin.settings().paymentAllowed(option.item().getType(), shop)) {
            return 0;
        }
        long needPerUnit = option.amount();
        if (needPerUnit <= 0 || needPerUnit > 64) {
            return 0;
        }
        int stock = HARD_CAP;
        if (!shop.isAdmin()) {
            Chest chest = plugin.shops().chest(shop);
            if (chest == null) {
                return 0;
            }
            stock = Items.count(chest.getInventory(), listing.item());
        }
        int payment = Items.count(buyer.getInventory(), option.item());
        int room = Items.room(buyer.getInventory(), listing.item());
        int affordable = (int) Math.min(HARD_CAP, payment / needPerUnit);
        return Math.max(0, Math.min(stock, Math.min(affordable, room)));
    }

    /** Executes one purchase. Returns true only when every step succeeded. */
    public boolean execute(Player buyer, Shop shop, Listing listing, int optionIndex, int quantity) {
        String key = shop.id() + ":" + listing.id();
        if (!locked.add(key)) {
            plugin.msg().send(buyer, "trade-failed");
            return false;
        }
        try {
            return executeLocked(buyer, shop, listing, optionIndex, quantity);
        } finally {
            locked.remove(key);
        }
    }

    private boolean executeLocked(Player buyer, Shop shop, Listing listing, int optionIndex, int quantity) {
        if (buyer == null || !buyer.isOnline() || shop == null || !shop.open() || listing == null) {
            fail(buyer);
            return false;
        }
        if (shop.ownedBy(buyer.getUniqueId())) {
            plugin.msg().send(buyer, "self-trade");
            return false;
        }
        PaymentOption option = listing.option(optionIndex);
        if (option == null || quantity <= 0 || quantity > HARD_CAP) {
            fail(buyer);
            return false;
        }
        if (!plugin.settings().paymentAllowed(option.item().getType(), shop)) {
            plugin.msg().send(buyer, shop.isAdmin() ? "payment-override-blocked" : "payment-blacklisted");
            return false;
        }
        int max = maxQuantity(buyer, shop, listing, optionIndex);
        if (quantity > max) {
            if (max <= 0 && !shop.isAdmin()) {
                plugin.msg().send(buyer, "not-enough-stock");
            } else {
                plugin.msg().send(buyer, "not-enough-payment");
            }
            return false;
        }
        long requiredLong = (long) option.amount() * quantity;
        if (requiredLong <= 0 || requiredLong > Integer.MAX_VALUE) {
            fail(buyer);
            return false;
        }
        int required = (int) requiredLong;

        Chest chest = shop.isAdmin() ? null : plugin.shops().chest(shop);
        if (!shop.isAdmin() && chest == null) {
            plugin.msg().send(buyer, "chest-not-found");
            return false;
        }
        if (!shop.isAdmin() && Items.collectRoom(shop.collect(), option.item(),
                plugin.settings().collectSlots()) < required) {
            plugin.msg().send(buyer, "collect-full");
            return false;
        }

        // Snapshot everything we are about to touch. All of this is main-thread only.
        ItemStack[] buyerBefore = Items.cloneContents(buyer.getInventory().getStorageContents());
        ItemStack[] chestBefore = chest == null ? null
                : Items.cloneContents(chest.getInventory().getStorageContents());
        java.util.List<ItemStack> collectBefore = shop.collect();

        boolean ok = false;
        try {
            int paid = Items.remove(buyer.getInventory(), option.item(), required);
            if (paid != required) {
                plugin.msg().send(buyer, "not-enough-payment");
                return false;
            }
            if (chest != null) {
                int taken = Items.remove(chest.getInventory(), listing.item(), quantity);
                if (taken != quantity) {
                    plugin.msg().send(buyer, "not-enough-stock");
                    return false;
                }
            }
            ItemStack[] gifts = Items.stacks(listing.item(), quantity).toArray(new ItemStack[0]);
            Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(gifts);
            if (!leftovers.isEmpty()) {
                plugin.msg().send(buyer, "no-room", "item", Items.name(listing.item()));
                return false;
            }
            if (!plugin.shops().collectAdd(shop, option.item(), required)) {
                plugin.msg().send(buyer, "collect-full");
                return false;
            }
            plugin.storage().save();
            plugin.logs().log(buyer.getName(),
                    shop.isAdmin() ? "admin:" + shop.name() : plugin.shops().ownerName(shop),
                    Items.describe(listing.item(), quantity),
                    Items.describe(option.item(), required),
                    describeLocation(shop));
            plugin.msg().send(buyer, "trade-success",
                    "item", Items.describe(listing.item(), quantity),
                    "payment", Items.describe(option.item(), required));
            ok = true;
            return true;
        } finally {
            if (!ok) {
                buyer.getInventory().setStorageContents(buyerBefore);
                if (chest != null && chestBefore != null) {
                    chest.getInventory().setStorageContents(chestBefore);
                }
                shop.collect(collectBefore);
            }
        }
    }

    private void fail(Player buyer) {
        if (buyer != null) {
            plugin.msg().send(buyer, "trade-failed");
        }
    }

    private String describeLocation(Shop shop) {
        if (shop.location() == null || shop.location().getWorld() == null) {
            return "unknown";
        }
        return shop.location().getWorld().getName() + " " + shop.location().getBlockX()
                + "," + shop.location().getBlockY() + "," + shop.location().getBlockZ();
    }
}
