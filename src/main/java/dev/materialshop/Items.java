package dev.materialshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exact ItemStack matching and inventory arithmetic. Never rebuild an item from its
 * material: every transfer uses clones of the original ItemStack, carrying all NBT.
 */
public final class Items {
    private Items() {
    }

    /** Exact match including name, lore, enchantments, custom model data and NBT. */
    public static boolean same(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.getType() != b.getType()) {
            return false;
        }
        ItemStack left = a.clone();
        ItemStack right = b.clone();
        left.setAmount(1);
        right.setAmount(1);
        if (!left.equals(right)) {
            return false;
        }
        Map<String, Object> leftMap = left.serialize();
        Map<String, Object> rightMap = right.serialize();
        return leftMap.equals(rightMap);
    }

    public static int count(Inventory inventory, ItemStack template) {
        int total = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (same(slot, template)) {
                total += slot.getAmount();
            }
        }
        return total;
    }

    /** Removes up to amount matching items. Returns how many were actually removed. */
    public static int remove(Inventory inventory, ItemStack template, int amount) {
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (!same(slot, template)) {
                continue;
            }
            int take = Math.min(remaining, slot.getAmount());
            if (take == slot.getAmount()) {
                contents[i] = null;
            } else {
                ItemStack copy = slot.clone();
                copy.setAmount(slot.getAmount() - take);
                contents[i] = copy;
            }
            remaining -= take;
        }
        inventory.setStorageContents(contents);
        return amount - remaining;
    }

    /** How many more of this exact item fit using only the inventory's storage slots. */
    public static int room(Inventory inventory, ItemStack template) {
        int max = Math.max(1, template.getMaxStackSize());
        int freeSlots = 0;
        int sameItemRoom = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null) {
                freeSlots++;
            } else if (same(slot, template)) {
                sameItemRoom += Math.max(0, max - slot.getAmount());
            }
        }
        return freeSlots * max + sameItemRoom;
    }

    public static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] out = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            out[i] = contents[i] == null ? null : contents[i].clone();
        }
        return out;
    }

    /** Splits an exact item into vanilla-sized stacks, preserving all item data. */
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

    /** How many collect-box slots are free for this exact item. */
    public static int collectRoom(List<ItemStack> existing, ItemStack template, int capacity) {
        int freeSlots = Math.max(0, capacity - existing.size());
        int max = Math.max(1, template.getMaxStackSize());
        int room = freeSlots * max;
        for (ItemStack item : existing) {
            if (same(item, template)) {
                room += Math.max(0, max - item.getAmount());
            }
        }
        return room;
    }

    /** Adds to a COPY of the collect list. Returns items that did not fit. */
    public static List<ItemStack> collectAdd(List<ItemStack> source, ItemStack template, int amount,
                                             int capacity) {
        List<ItemStack> working = new ArrayList<>();
        for (ItemStack item : source) {
            working.add(item.clone());
        }
        int left = amount;
        int max = Math.max(1, template.getMaxStackSize());
        for (ItemStack item : working) {
            if (left <= 0) {
                break;
            }
            if (same(item, template) && item.getAmount() < max) {
                int add = Math.min(left, max - item.getAmount());
                item.setAmount(item.getAmount() + add);
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
        source.clear();
        source.addAll(working);
        if (left > 0) {
            return stacks(template, left);
        }
        return new ArrayList<>();
    }

    /** Plain-text item name for messages and the transaction log. */
    public static String name(ItemStack item) {
        if (item == null) {
            return "air";
        }
        String label = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        if (item.hasItemMeta()) {
            Component display = item.getItemMeta().displayName();
            if (display != null) {
                String custom = PlainTextComponentSerializer.plainText().serialize(display);
                if (!custom.isBlank()) {
                    return custom;
                }
            }
        }
        return label;
    }

    public static String describe(ItemStack template, int amount) {
        return amount + " x " + name(template);
    }
}
