package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.PaymentOption;
import com.banglaverse.materialshop.model.Shop;
import com.banglaverse.materialshop.trade.Rewards;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Setup menu with controlled free slots for the sale item and up to four payments. */
public final class SetupMenu extends MenuBase {
    public enum Mode { PLAYER, SERVER, CHEST }

    private static final int SALE_SLOT = 10;
    private static final int[] PAY_SLOTS = {12, 13, 14, 15};

    private final Mode mode;
    private final UUID editId;
    private final UUID shopId;
    private final Location chestLocation;
    private final String shopName;
    private final Set<Integer> prefilled = new HashSet<>();
    private final Set<Integer> freeSlots = new HashSet<>();
    private ItemStack saleItem;
    private final List<ItemStack> payments = new ArrayList<>();
    private int units = 1;
    private boolean confirmed;
    private boolean returned;

    public SetupMenu(MaterialShop plugin, Player player) {
        this(plugin, player, Mode.PLAYER, null, null, null, null);
    }

    public SetupMenu(MaterialShop plugin, Player player, Mode mode, UUID shopId,
                     Location chestLocation, String shopName, UUID editId) {
        super(plugin, player);
        this.mode = mode;
        this.shopId = shopId;
        this.chestLocation = chestLocation;
        this.shopName = shopName;
        this.editId = editId;
        freeSlots.add(SALE_SLOT);
        freeSlots.addAll(Arrays.stream(PAY_SLOTS).boxed().toList());
        if (editId != null) {
            Listing listing = plugin.data().listing(editId);
            if (listing != null) {
                saleItem = listing.item();
                prefilled.add(SALE_SLOT);
                int i = 0;
                for (PaymentOption option : listing.options()) {
                    payments.add(option.item());
                    prefilled.add(PAY_SLOTS[i++]);
                }
                units = Math.max(1, listing.stock());
            }
        }
    }

    private boolean isFree(int slot) {
        return freeSlots.contains(slot);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (isFree(event.getRawSlot())
                && (event.getClick().name().equals("LEFT") || event.getClick().name().equals("RIGHT"))) {
            Bukkit.getScheduler().runTask(plugin, this::syncFree);
            return;
        }
        super.handleClick(event);
    }

    @Override
    public void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        event.setCancelled(true);
    }

    private void syncFree() {
        saleItem = inv.getItem(SALE_SLOT) == null ? null : inv.getItem(SALE_SLOT).clone();
        payments.clear();
        for (int slot : PAY_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                payments.add(item.clone());
            }
        }
    }

    /** Returns samples and placed items when the menu closes without confirming. */
    @Override
    public void onClose() {
        if (returned || confirmed) {
            return;
        }
        returned = true;
        List<ItemStack> toReturn = new ArrayList<>();
        if (saleItem != null && !prefilled.contains(SALE_SLOT)) {
            toReturn.add(saleItem.clone());
        }
        int i = 0;
        for (ItemStack payment : payments) {
            if (!prefilled.contains(PAY_SLOTS[i])) {
                toReturn.add(payment.clone());
            }
            i++;
        }
        Rewards.give(plugin, player.getUniqueId(), toReturn);
    }

    @Override
    protected int size() {
        return 27;
    }

    @Override
    protected String titleKey() {
        return "menu.setup.title";
    }

    @Override
    protected void build() {
        set(SALE_SLOT, saleItem == null ? icon(Material.ITEM_FRAME, "menu.setup.sale") : saleItem.clone());
        for (int i = 0; i < PAY_SLOTS.length; i++) {
            ItemStack payment = i < payments.size() ? payments.get(i).clone()
                    : icon(Material.GOLD_INGOT, "menu.setup.payment", "index", String.valueOf(i + 1));
            set(PAY_SLOTS[i], payment);
        }
        String note = mode == Mode.SERVER ? "menu.setup.server-note"
                : mode == Mode.CHEST ? "menu.setup.chest-note" : null;
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (editId != null) {
            lore.add(plugin.msg().plain("menu.setup.edit-note"));
        }
        if (note != null) {
            lore.add(plugin.msg().plain(note));
        }
        lore.add(plugin.msg().plain("menu.setup.return-line"));
        set(21, itemIcon(new ItemStack(Material.PAPER), "menu.setup.units", lore,
                "amount", String.valueOf(units)), this::promptUnits);
        set(22, icon(Material.LEVER, "menu.setup.plus-one"), () -> addUnits(1));
        set(23, icon(Material.LEVER, "menu.setup.plus-eight"), () -> addUnits(8));
        set(25, icon(Material.EMERALD_BLOCK, "menu.setup.confirm"), this::confirm);
        set(26, icon(Material.BARRIER, "menu.common.close"), player::closeInventory);
    }

    private void addUnits(int amount) {
        units = Math.min(plugin.settings().maxQuantity(), Math.max(1, units + amount));
        rebuild();
    }

    private void promptUnits() {
        int max = mode == Mode.SERVER ? plugin.settings().maxQuantity() : availableUnits();
        plugin.msg().send(player, "stock-prompt");
        plugin.prompts().request(player, text -> {
            int value;
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", String.valueOf(max));
                return;
            }
            if (value < 1 || value > max) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", String.valueOf(max));
                return;
            }
            units = value;
            rebuild();
        });
    }

    private int availableUnits() {
        if (saleItem == null || saleItem.getType().isAir()) {
            return 0;
        }
        return Items.count(player.getInventory(), saleItem) / Math.max(1, saleItem.getAmount());
    }

    private void confirm() {
        syncFree();
        if (saleItem == null || saleItem.getType().isAir()) {
            plugin.msg().send(player, "no-sale-item");
            return;
        }
        if (payments.isEmpty()) {
            plugin.msg().send(player, "no-payment");
            return;
        }
        if (payments.size() > 4) {
            plugin.msg().send(player, "too-many-payments");
            return;
        }
        for (ItemStack payment : payments) {
            if (!plugin.settings().paymentAllowed(payment.getType(), mode == Mode.SERVER)) {
                plugin.msg().send(player, "payment-blacklisted");
                return;
            }
        }
        if (editId != null) {
            Listing listing = plugin.data().listing(editId);
            if (listing == null) {
                plugin.msg().send(player, "listing-gone");
                return;
            }
            List<PaymentOption> options = new ArrayList<>();
            for (ItemStack payment : payments) {
                options.add(new PaymentOption(payment));
            }
            listing.options(options);
            plugin.data().saveListing(listing);
            plugin.msg().send(player, "listing-updated");
            confirmed = true;
            player.closeInventory();
            return;
        }
        if (mode == Mode.PLAYER
                && plugin.data().listingsByOwner(player.getUniqueId()).size() >= plugin.settings().maxListings()) {
            plugin.msg().send(player, "limit-reached", "limit", String.valueOf(plugin.settings().maxListings()));
            return;
        }
        if (mode == Mode.CHEST
                && plugin.data().chestShopCount(player.getUniqueId()) >= plugin.settings().shopLimit(player)) {
            plugin.msg().send(player, "shop-limit-reached", "limit", String.valueOf(plugin.settings().shopLimit(player)));
            return;
        }
        if (mode == Mode.PLAYER) {
            int available = availableUnits();
            if (units > available) {
                plugin.msg().send(player, "invalid-number", "min", "1", "max", String.valueOf(available));
                return;
            }
            int needed = units * saleItem.getAmount();
            if (Items.remove(player.getInventory(), saleItem, needed) != needed) {
                plugin.msg().send(player, "trade-failed");
                return;
            }
        }
        Listing.Source source = mode == Mode.SERVER ? Listing.Source.SERVER
                : mode == Mode.CHEST ? Listing.Source.CHEST : Listing.Source.PLAYER;
        UUID owner = mode == Mode.SERVER ? null : player.getUniqueId();
        UUID targetShop = shopId;
        if (mode == Mode.CHEST && targetShop == null && chestLocation != null) {
            Shop shop = new Shop(UUID.randomUUID(), Shop.Type.CHEST, player.getUniqueId(),
                    shopName == null ? player.getName() + "'s shop" : shopName, chestLocation);
            plugin.data().saveShop(shop);
            targetShop = shop.id();
        }
        Listing listing = new Listing(UUID.randomUUID(), owner, source, targetShop, saleItem,
                (int) plugin.settings().expiryDays());
        List<PaymentOption> options = new ArrayList<>();
        for (ItemStack payment : payments) {
            options.add(new PaymentOption(payment));
            int suggested = plugin.settings().suggested(saleItem, payment);
            if (suggested > 0 && plugin.settings().unfair(payment.getAmount(), saleItem, payment)) {
                plugin.msg().send(player, "fairness-warning", "ratio",
                        String.format(java.util.Locale.ROOT, "%.1f", (double) payment.getAmount() / suggested),
                        "suggested", String.valueOf(suggested), "item", Items.name(payment));
            }
        }
        listing.options(options);
        if (mode == Mode.PLAYER) {
            for (int i = 0; i < units; i++) {
                listing.addUnit(saleItem);
            }
        }
        plugin.data().saveListing(listing);
        confirmed = true;
        // Payment samples are copies of what the player placed; give them back.
        List<ItemStack> samples = new ArrayList<>();
        for (int i = 0; i < payments.size(); i++) {
            if (!prefilled.contains(PAY_SLOTS[i])) {
                samples.add(payments.get(i).clone());
            }
        }
        if (!prefilled.contains(SALE_SLOT)) {
            samples.add(saleItem.clone());
        }
        Rewards.give(plugin, player.getUniqueId(), samples);
        plugin.msg().send(player, mode == Mode.CHEST ? "chest-shop-created" : "listing-created",
                "item", Items.describe(saleItem, saleItem.getAmount()));
        player.closeInventory();
    }
}
