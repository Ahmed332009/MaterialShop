package dev.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shop lifecycle, chest stock, ownership and the virtual collect box. */
public final class ShopManager {
    private final MaterialShop plugin;

    public ShopManager(MaterialShop plugin) {
        this.plugin = plugin;
    }

    public Collection<Shop> all() {
        return plugin.storage().all().values();
    }

    public Shop get(UUID id) {
        return plugin.storage().get(id);
    }

    public List<Shop> adminShops() {
        List<Shop> out = new ArrayList<>();
        for (Shop shop : all()) {
            if (shop.isAdmin()) {
                out.add(shop);
            }
        }
        return out;
    }

    public List<Shop> playerShops() {
        List<Shop> out = new ArrayList<>();
        for (Shop shop : all()) {
            if (!shop.isAdmin()) {
                out.add(shop);
            }
        }
        return out;
    }

    public List<Shop> ownedBy(UUID owner) {
        List<Shop> out = new ArrayList<>();
        for (Shop shop : all()) {
            if (shop.ownedBy(owner)) {
                out.add(shop);
            }
        }
        return out;
    }

    public void save(Shop shop) {
        plugin.storage().put(shop);
    }

    public Shop createAdmin(String name, Location location) {
        Shop shop = new Shop(UUID.randomUUID(), Shop.Type.ADMIN, null, name, location);
        save(shop);
        return shop;
    }

    /** Creates a player shop. The held item becomes the first listing's exact template. */
    public Shop createPlayer(Player player, Block chestBlock, ItemStack held, String name) {
        if (held == null || held.getType().isAir()) {
            plugin.msg().send(player, "create-no-item");
            return null;
        }
        if (!allowedChest(chestBlock)) {
            plugin.msg().send(player, "create-not-chest");
            return null;
        }
        int limit = plugin.settings().shopLimit(player);
        int owned = plugin.storage().countOwned(player.getUniqueId());
        if (owned >= limit) {
            plugin.msg().send(player, "limit-reached", "count", String.valueOf(owned),
                    "limit", String.valueOf(limit));
            return null;
        }
        String shopName = name == null || name.isBlank() ? player.getName() + "'s shop" : name;
        Location location = chestBlock.getLocation();
        Shop shop = new Shop(UUID.randomUUID(), Shop.Type.PLAYER, player.getUniqueId(), shopName, location);
        Listing listing = new Listing(UUID.randomUUID(), held);
        shop.listings().add(listing);
        save(shop);
        plugin.msg().send(player, "created", "name", shop.name());
        return shop;
    }

    public boolean allowedChest(Block block) {
        return block != null && block.getState() instanceof Chest;
    }

    /** Live chest, or null when it was removed or the chunk is unloaded. */
    public Chest chest(Shop shop) {
        if (shop == null || shop.isAdmin() || shop.location() == null) {
            return null;
        }
        Block block = shop.location().getWorld().getBlockAt(shop.location());
        return block.getState() instanceof Chest chest ? chest : null;
    }

    public int stock(Shop shop, Listing listing) {
        if (shop == null || shop.isAdmin() || listing == null) {
            return 0;
        }
        Chest chest = chest(shop);
        return chest == null ? 0 : Items.count(chest.getInventory(), listing.item());
    }

    public boolean mayManage(Player player, Shop shop) {
        if (shop == null) {
            return false;
        }
        return player.hasPermission("materialshop.admin")
                || shop.ownedBy(player.getUniqueId());
    }

    public String ownerName(Shop shop) {
        if (shop == null || shop.isAdmin()) {
            return plugin.msg().string("word-server", "Server");
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.owner());
        return owner.getName() == null ? shop.owner().toString().substring(0, 8) : owner.getName();
    }

    /** Adds payment to the owner's (or admin shop's) collect box, atomically in memory. */
    public boolean collectAdd(Shop shop, ItemStack template, int amount) {
        List<ItemStack> working = shop.collect();
        List<ItemStack> leftover = Items.collectAdd(working, template, amount, plugin.settings().collectSlots());
        if (!leftover.isEmpty()) {
            return false;
        }
        shop.collect(working);
        return true;
    }

    /** Takes as much as fits from a collect box. Returns the number of stacks taken. */
    public int collectTakeAll(Shop shop, Player player) {
        List<ItemStack> box = shop.collect();
        List<ItemStack> remaining = new ArrayList<>();
        int taken = 0;
        for (ItemStack item : box) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            if (leftovers.isEmpty()) {
                taken++;
            } else {
                remaining.addAll(leftovers.values());
            }
        }
        if (taken > 0) {
            shop.collect(remaining);
            save(shop);
        }
        return taken;
    }

    /** Returns all chest stock and collect-box items to the owner when a shop closes. */
    public void returnAll(Shop shop) {
        if (shop.isAdmin()) {
            return;
        }
        Chest chest = chest(shop);
        List<ItemStack> returned = new ArrayList<>();
        if (chest != null) {
            for (ItemStack item : chest.getInventory().getStorageContents()) {
                if (item != null && !item.getType().isAir()) {
                    returned.add(item.clone());
                }
            }
            chest.getInventory().clear();
        }
        List<ItemStack> box = shop.collect();
        Player owner = Bukkit.getPlayer(shop.owner());
        if (owner != null && owner.isOnline()) {
            List<ItemStack> leftovers = new ArrayList<>();
            for (ItemStack item : returned) {
                for (ItemStack rest : owner.getInventory().addItem(item).values()) {
                    leftovers.add(rest);
                }
            }
            for (ItemStack item : box) {
                for (ItemStack rest : owner.getInventory().addItem(item).values()) {
                    leftovers.add(rest);
                }
            }
            shop.collect(leftovers);
        } else {
            List<ItemStack> merged = new ArrayList<>(box);
            List<ItemStack> overflow = new ArrayList<>();
            for (ItemStack item : returned) {
                overflow.addAll(Items.collectAdd(merged, item, item.getAmount(), plugin.settings().collectSlots()));
            }
            shop.collect(merged);
            if (!overflow.isEmpty()) {
                if (chest != null) {
                    for (ItemStack item : chest.getInventory().addItem(overflow.toArray(new ItemStack[0])).values()) {
                        drop(item);
                    }
                } else {
                    for (ItemStack item : overflow) {
                        drop(item);
                    }
                }
            }
        }
        save(shop);
    }

    /** Last-resort return path when both the chest and collect box are full. */
    private void drop(ItemStack item) {
        Location at = shopLocationOrSpawn();
        at.getWorld().dropItemNaturally(at, item);
        plugin.getLogger().warning("Shop overflow returned as a dropped item at "
                + at.getWorld().getName() + " " + at.getBlockX() + ", " + at.getBlockY() + ", "
                + at.getBlockZ() + ": " + Items.describe(item, item.getAmount()));
    }

    private Location shopLocationOrSpawn() {
        for (Shop shop : all()) {
            if (shop.location() != null) {
                return shop.location();
            }
        }
        return plugin.getServer().getWorlds().get(0).getSpawnLocation();
    }
}
