package com.banglaverse.materialshop.model;

import org.bukkit.inventory.ItemStack;

/** One accepted payment item template and the exact stack size required. */
public final class PaymentOption {
    private ItemStack item;

    public PaymentOption(ItemStack item) {
        this.item = item.clone();
        this.item.setAmount(Math.max(1, item.getAmount()));
    }

    public ItemStack item() {
        return item.clone();
    }

    /** Replaces the required amount while keeping all item data. */
    public void amount(int value) {
        ItemStack copy = item.clone();
        copy.setAmount(Math.max(1, value));
        item = copy;
    }

    public int amount() {
        return item.getAmount();
    }
}
