package com.banglaverse.materialshop.chest;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.CollectBox;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.Shop;
import com.banglaverse.materialshop.trade.Rewards;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stock and access rules for the optional chest-shop module. */
public final class ChestShopManager {
    private final MaterialShop plugin;

    public ChestShopManager(MaterialShop plugin) {
        this.plugin = plugin;
    }

    public Shop shopAt(Block block) {
        if (block == null) {
            return null;
        }
        for (Shop shop : plugin.data().shops(Shop.Type.CHEST)) {
            if (shop.location() != null && shop.location().getWorld().equals(block.getWorld())
                    && shop.location().getBlockX() == block.getX()
                    && shop.location().getBlockY() == block.getY()
                    && shop.location().getBlockZ() == block.getZ()) {
                return shop;
            }
        }
        return null;
    }

    public Container container(Shop shop) {
        if (shop == null || shop.location() == null) {
            return null;
        }
        Block block = shop.location().getBlock();
        return block.getState() instanceof Container container ? container : null;
    }

    public int stock(Listing listing) {
        Shop shop = plugin.data().shop(listing.shopId());
        Container container = container(shop);
        return container == null ? 0 : Items.count(container.getInventory(), listing.item());
    }

    /** Takes exact matching items out of the container. */
    public List<ItemStack> withdraw(Listing listing, int amount) {
        Shop shop = plugin.data().shop(listing.shopId());
        Container container = container(shop);
        List<ItemStack> taken = new ArrayList<>();
        if (container == null) {
            return taken;
        }
        int left = amount;
        var contents = Items.cloneContents(container.getInventory().getStorageContents());
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack slot = contents[i];
            if (!Items.similar(slot, listing.item())) {
                continue;
            }
            int take = Math.min(left, slot.getAmount());
            ItemStack copy = slot.clone();
            copy.setAmount(take);
            taken.add(copy);
            if (take == slot.getAmount()) {
                contents[i] = null;
            } else {
                ItemStack rest = slot.clone();
                rest.setAmount(slot.getAmount() - take);
                contents[i] = rest;
            }
            left -= take;
        }
        if (left == 0) {
            container.getInventory().setStorageContents(contents);
        }
        return left == 0 ? taken : new ArrayList<>();
    }

    public void deposit(Listing listing, List<ItemStack> stacks) {
        Shop shop = plugin.data().shop(listing.shopId());
        Container container = container(shop);
        if (container != null) {
            for (ItemStack stack : stacks) {
                container.getInventory().addItem(stack.clone());
            }
        }
    }

    /** Returns container stock and shop earnings to the owner's collect box. */
    public void close(Shop shop) {
        Container container = container(shop);
        CollectBox box = plugin.data().box(shop.owner());
        if (container != null) {
            for (ItemStack item : container.getInventory().getStorageContents()) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                Rewards.give(plugin, shop.owner(), box.add(item, item.getAmount(),
                        plugin.settings().collectCapacity()));
            }
            container.getInventory().clear();
        }
        for (Listing listing : plugin.data().listingsByShop(shop.id())) {
            plugin.data().removeListing(listing.id());
        }
        plugin.data().removeShop(shop.id());
    }
}
