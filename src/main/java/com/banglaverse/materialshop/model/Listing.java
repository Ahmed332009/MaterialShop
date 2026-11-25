package com.banglaverse.materialshop.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An exact item sold for 1-4 payment options, with optional escrow stock. */
public final class Listing {
    public enum Source { PLAYER, SERVER, CHEST }

    private final UUID id;
    private final UUID owner;
    private final Source source;
    private final UUID shopId;
    private ItemStack item;
    private List<PaymentOption> options = new ArrayList<>();
    private List<ItemStack> escrow = new ArrayList<>();
    private final long created;
    private long expiresAt;

    public Listing(UUID id, UUID owner, Source source, UUID shopId, ItemStack item, int expiryDays) {
        this.id = id;
        this.owner = owner;
        this.source = source;
        this.shopId = shopId;
        this.item = item.clone();
        this.item.setAmount(Math.max(1, item.getAmount()));
        this.created = System.currentTimeMillis();
        this.expiresAt = expiryDays > 0 ? created + expiryDays * 24L * 60L * 60L * 1000L : 0L;
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public Source source() {
        return source;
    }

    public UUID shopId() {
        return shopId;
    }

    public ItemStack item() {
        return item.clone();
    }

    public PaymentOption option(int index) {
        return index >= 0 && index < options.size() ? new PaymentOption(options.get(index).item()) : null;
    }

    public void options(List<PaymentOption> values) {
        options = new ArrayList<>(values);
    }

    public List<PaymentOption> options() {
        List<PaymentOption> out = new ArrayList<>();
        for (PaymentOption option : options) {
            out.add(new PaymentOption(option.item()));
        }
        return out;
    }

    public void escrow(List<ItemStack> stacks) {
        escrow = new ArrayList<>();
        for (ItemStack stack : stacks) {
            escrow.add(stack.clone());
        }
    }

    /** One escrow stack is one purchase unit. */
    public List<ItemStack> escrow() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : escrow) {
            out.add(stack.clone());
        }
        return out;
    }

    public int stock() {
        return source == Source.SERVER ? Integer.MAX_VALUE : escrow.size();
    }

    /** Removes one purchase unit from escrow. Returns null when empty. */
    public ItemStack takeUnit() {
        return escrow.isEmpty() ? null : escrow.remove(escrow.size() - 1).clone();
    }

    /** Adds one purchase unit, preserving all item data. */
    public void addUnit(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(Math.max(1, item.getAmount()));
        escrow.add(copy);
    }

    public long created() {
        return created;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public void expiresAt(long value) {
        this.expiresAt = value;
    }

    public boolean expired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }
}
