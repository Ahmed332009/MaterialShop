package com.banglaverse.materialshop.trade;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.model.CollectBox;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Safe return path for escrow, setup samples and collect-box overflow. */
public final class Rewards {
    private Rewards() {
    }

    /** Gives items to an online owner, then the collect box. Nothing is silently dropped. */
    public static void give(MaterialShop plugin, UUID owner, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        Player player = Bukkit.getPlayer(owner);
        List<ItemStack> left = new ArrayList<>();
        if (player != null && player.isOnline()) {
            for (ItemStack item : items) {
                left.addAll(player.getInventory().addItem(item.clone()).values());
            }
        } else {
            left.addAll(items);
        }
        CollectBox box = plugin.data().box(owner);
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack item : left) {
            overflow.addAll(box.add(item, item.getAmount(), plugin.settings().collectCapacity()));
        }
        for (ItemStack item : overflow) {
            if (player != null && player.isOnline()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            plugin.getLogger().warning("MaterialShop overflow item kept as a drop: " + Items.describe(item, item.getAmount()));
        }
        plugin.data().save();
    }
}
