package com.banglaverse.materialshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Exact ItemStack matching and safe inventory arithmetic; never rebuilds an item. */
public final class Items {
    private Items() {
    }

    /** Required similarity check: names, enchants and custom data all have to match. */
    public static boolean similar(ItemStack a, ItemStack b) {
        return a != null && b != null && !a.getType().isAir() && a.isSimilar(b);
    }

    public static int count(Inventory inventory, ItemStack template) {
        int total = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (similar(slot, template)) {
                total += slot.getAmount();
            }
        }
        return total;
    }

    /** Removes up to amount matching items and returns how many were removed. */
    public static int remove(Inventory inventory, ItemStack template, int amount) {
        ItemStack[] contents = cloneContents(inventory.getStorageContents());
        int left = amount;
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack slot = contents[i];
            if (!similar(slot, template)) {
                continue;
            }
            int take = Math.min(left, slot.getAmount());
            if (take == slot.getAmount()) {
                contents[i] = null;
            } else {
                ItemStack copy = slot.clone();
                copy.setAmount(slot.getAmount() - take);
                contents[i] = copy;
            }
            left -= take;
        }
        inventory.setStorageContents(contents);
        return amount - left;
    }

    /** How many more of this exact item fit into the storage slots. */
    public static int room(Inventory inventory, ItemStack template) {
        int max = Math.max(1, template.getMaxStackSize());
        int free = 0;
        int sameRoom = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null) {
                free++;
            } else if (similar(slot, template)) {
                sameRoom += Math.max(0, max - slot.getAmount());
            }
        }
        return free * max + sameRoom;
    }

    public static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] out = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            out[i] = contents[i] == null ? null : contents[i].clone();
        }
        return out;
    }

    public static List<ItemStack> stacks(ItemStack template, int amount) {
        List<ItemStack> out = new ArrayList<>();
        int max = Math.max(1, template.getMaxStackSize());
        int left = amount;
        while (left > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(max, left));
            out.add(stack);
            left -= stack.getAmount();
        }
        return out;
    }

    /** Free room in a virtual collect box, counted in individual items. */
    public static int collectRoom(List<ItemStack> stacks, ItemStack template, int capacity) {
        int max = Math.max(1, template.getMaxStackSize());
        int room = Math.max(0, capacity - stacks.size()) * max;
        for (ItemStack stack : stacks) {
            if (similar(stack, template)) {
                room += Math.max(0, max - stack.getAmount());
            }
        }
        return room;
    }

    /** Plain-text name for messages and logs. */
    public static String name(ItemStack item) {
        if (item == null) {
            return "air";
        }
        if (item.hasItemMeta()) {
            Component display = item.getItemMeta().displayName();
            if (display != null) {
                String text = PlainTextComponentSerializer.plainText().serialize(display);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public static String describe(ItemStack template, int amount) {
        return amount + " x " + name(template);
    }
}
