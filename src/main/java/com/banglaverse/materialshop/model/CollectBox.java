package com.banglaverse.materialshop.model;

import com.banglaverse.materialshop.Items;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Virtual chest holding a player's sale proceeds. */
public final class CollectBox {
    private final List<ItemStack> stacks = new ArrayList<>();

    public List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            out.add(stack.clone());
        }
        return out;
    }

    public void stacks(List<ItemStack> values) {
        stacks.clear();
        if (values != null) {
            for (ItemStack stack : values) {
                if (stack != null && !stack.getType().isAir()) {
                    stacks.add(stack.clone());
                }
            }
        }
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public boolean canHold(ItemStack template, int amount, int capacity) {
        return Items.collectRoom(stacks(), template, capacity) >= amount;
    }

    /** Adds items to a copy of the box, returning anything that did not fit. */
    public List<ItemStack> add(ItemStack template, int amount, int capacity) {
        List<ItemStack> working = stacks();
        List<ItemStack> leftover = addTo(working, template, amount, capacity);
        stacks(working);
        return leftover;
    }

    public static List<ItemStack> addTo(List<ItemStack> working, ItemStack template, int amount, int capacity) {
        int left = amount;
        int max = Math.max(1, template.getMaxStackSize());
        for (ItemStack stack : working) {
            if (left <= 0) {
                break;
            }
            if (Items.similar(stack, template) && stack.getAmount() < max) {
                int add = Math.min(left, max - stack.getAmount());
                stack.setAmount(stack.getAmount() + add);
                left -= add;
            }
        }
        while (left > 0 && working.size() < capacity) {
            ItemStack stack = template.clone();
            int add = Math.min(left, max);
            stack.setAmount(add);
            working.add(stack);
            left -= add;
        }
        return left > 0 ? Items.stacks(template, left) : new ArrayList<>();
    }

    /** Gives everything that fits. Returns the number of stacks taken. */
    public int takeAll(org.bukkit.entity.Player player) {
        int taken = 0;
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : stacks()) {
            if (player.getInventory().addItem(stack.clone()).isEmpty()) {
                taken++;
            } else {
                remaining.add(stack);
            }
        }
        if (taken > 0) {
            stacks(remaining);
        }
        return taken;
    }
}
