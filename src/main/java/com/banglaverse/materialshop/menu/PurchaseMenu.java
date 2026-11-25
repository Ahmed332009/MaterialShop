package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.PaymentOption;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** You-give / you-get purchase screen with payment, quantity and confirm controls. */
public final class PurchaseMenu extends MenuBase {
    private final UUID listingId;
    private int option;
    private int quantity = 1;

    public PurchaseMenu(MaterialShop plugin, Player player, UUID listingId) {
        super(plugin, player);
        this.listingId = listingId;
    }

    public PurchaseMenu(MaterialShop plugin, Player player, UUID listingId, int option, int quantity) {
        this(plugin, player, listingId);
        this.option = option;
        this.quantity = quantity;
    }

    private Listing fresh() {
        Listing listing = plugin.data().listing(listingId);
        if (listing == null || listing.expired()) {
            plugin.msg().send(player, "listing-gone");
            new MainMenu(plugin, player).open();
            return null;
        }
        return listing;
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String titleKey() {
        return "menu.purchase.title";
    }

    @Override
    protected void build() {
        Listing listing = fresh();
        if (listing == null || listing.options().isEmpty()) {
            return;
        }
        PaymentOption payment = listing.option(option);
        if (payment == null) {
            option = 0;
            payment = listing.option(0);
        }
        int max = plugin.trade().maxQuantity(player, listing, option);
        int qty = Math.max(1, Math.min(quantity, Math.max(1, max)));

        set(12, itemIcon(payment.item(), "menu.purchase.give",
                line("menu.purchase.give-line", "amount", String.valueOf(payment.amount() * qty),
                        "item", itemName(payment.item()))));
        ItemStack get = listing.item().clone();
        get.setAmount(Math.min(get.getMaxStackSize(), qty));
        set(14, itemIcon(get, "menu.purchase.get",
                line("menu.purchase.get-line", "amount", String.valueOf(qty), "item", itemName(listing.item()))));

        int index = 0;
        for (PaymentOption choice : listing.options()) {
            int chosen = index++;
            ItemStack icon = choice.item().clone();
            set(20 + chosen, itemIcon(icon, chosen == option ? "menu.purchase.option-active" : "menu.purchase.option",
                    line("menu.purchase.option-lore"), "item", itemName(choice.item())),
                    () -> new PurchaseMenu(plugin, player, listingId, chosen, qty).open());
        }

        set(37, icon(Material.LEVER, "menu.purchase.qty1"),
                () -> new PurchaseMenu(plugin, player, listingId, option, 1).open());
        set(38, icon(Material.LEVER, "menu.purchase.qty8"),
                () -> new PurchaseMenu(plugin, player, listingId, option, 8).open());
        set(39, icon(Material.LEVER, "menu.purchase.qty64"),
                () -> new PurchaseMenu(plugin, player, listingId, option, 64).open());
        set(40, icon(Material.GOLD_INGOT, "menu.purchase.max", "amount", String.valueOf(max)),
                () -> new PurchaseMenu(plugin, player, listingId, option, Math.max(1, max)).open());
        set(41, icon(Material.ANVIL, "menu.purchase.custom"), this::customQuantity);
        set(46, itemIcon(new ItemStack(max > 0 ? Material.EMERALD_BLOCK : Material.BARRIER),
                "menu.purchase.confirm",
                line("menu.purchase.confirm-lore", "payamount", String.valueOf(payment.amount() * qty),
                        "payitem", itemName(payment.item()), "getamount", String.valueOf(qty),
                        "item", itemName(listing.item()))),
                max > 0 ? this::confirm : null);
        set(49, icon(Material.PAPER, "menu.purchase.stock-line", "stock", String.valueOf(listing.stock())));
        set(50, icon(Material.PAPER, "menu.purchase.room-line", "room",
                String.valueOf(Items.room(player.getInventory(), listing.item()))));
        backToMain(53);
    }

    private void customQuantity() {
        Listing listing = fresh();
        if (listing == null) {
            return;
        }
        int max = Math.max(1, plugin.trade().maxQuantity(player, listing, option));
        plugin.msg().send(player, "quantity-prompt", "max", String.valueOf(max));
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
            new PurchaseMenu(plugin, player, listingId, option, value).open();
        });
    }

    private void confirm() {
        Listing listing = fresh();
        if (listing == null) {
            return;
        }
        plugin.trade().execute(player, listing, option, quantity);
        player.closeInventory();
    }
}
