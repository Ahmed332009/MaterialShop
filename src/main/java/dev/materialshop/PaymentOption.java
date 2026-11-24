package dev.materialshop;

import org.bukkit.inventory.ItemStack;

/** One accepted payment: an exact item template and how many of it are required. */
public final class PaymentOption {
    private ItemStack item;
    private int amount;

    public PaymentOption(ItemStack item, int amount) {
        this.item = normalize(item);
        this.amount = Math.max(1, Math.min(64, amount));
    }

    private static ItemStack normalize(ItemStack source) {
        ItemStack copy = source.clone();
        copy.setAmount(1);
        return copy;
    }

    public ItemStack item() {
        return item.clone();
    }

    public int amount() {
        return amount;
    }

    public void amount(int value) {
        this.amount = Math.max(1, Math.min(64, value));
    }

    public PaymentOption copy() {
        return new PaymentOption(item, amount);
    }
}
