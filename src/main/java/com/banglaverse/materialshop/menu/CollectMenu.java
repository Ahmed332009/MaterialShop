package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.model.CollectBox;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Virtual collect box that gives everything that fits in one tap. */
public final class CollectMenu extends MenuBase {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public CollectMenu(MaterialShop plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected String titleKey() {
        return "menu.collect.title";
    }

    @Override
    protected void build() {
        CollectBox box = plugin.data().box(player.getUniqueId());
        java.util.List<ItemStack> stacks = box.stacks();
        for (int i = 0; i < Math.min(SLOTS.length, stacks.size()); i++) {
            set(SLOTS[i], itemIcon(stacks.get(i), "menu.collect.item",
                    java.util.List.of(plugin.msg().plain("menu.collect.item"))));
        }
        if (stacks.isEmpty()) {
            set(31, icon(Material.BARRIER, "menu.collect.empty"));
        }
        set(49, icon(Material.HOPPER, "menu.collect.take-all"), () -> {
            int taken = box.takeAll(player);
            if (taken == 0) {
                plugin.msg().send(player, "collect-empty");
            } else {
                plugin.data().save();
                plugin.msg().send(player, "collected", "count", String.valueOf(taken));
            }
            new CollectMenu(plugin, player).open();
        });
        backToMain(53);
    }
}
