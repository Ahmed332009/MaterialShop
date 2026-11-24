package dev.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * YAML persistence, matching MaterialClaim's storage style. ItemStacks are stored with
 * Bukkit's own ConfigurationSerializable format, so names, enchantments, custom model
 * data and plugin NBT survive restarts byte-for-byte (no rebuilding from material).
 */
public final class ShopStorage {
    private final MaterialShop plugin;
    private final File file;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>();

    public ShopStorage(MaterialShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    public synchronized Map<UUID, Shop> all() {
        return new LinkedHashMap<>(shops);
    }

    public synchronized Shop get(UUID id) {
        return shops.get(id);
    }

    public synchronized void put(Shop shop) {
        shops.put(shop.id(), shop);
        save();
    }

    public synchronized void remove(UUID id) {
        if (shops.remove(id) != null) {
            save();
        }
    }

    public synchronized int countOwned(UUID owner) {
        int count = 0;
        for (Shop shop : shops.values()) {
            if (shop.ownedBy(owner)) {
                count++;
            }
        }
        return count;
    }

    public synchronized void load() {
        shops.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            UUID id = parseUuid(key);
            ConfigurationSection s = root.getConfigurationSection(key);
            if (id == null || s == null) {
                plugin.getLogger().warning("Skipping corrupt shop entry: " + key);
                continue;
            }
            try {
                Shop.Type type = "ADMIN".equalsIgnoreCase(s.getString("type", "PLAYER"))
                        ? Shop.Type.ADMIN : Shop.Type.PLAYER;
                UUID owner = parseUuid(s.getString("owner"));
                String name = s.getString("name", "Shop");
                Location location = readLocation(s.getConfigurationSection("location"));
                Shop shop = new Shop(id, type, owner, name, location);
                shop.entityUuid(parseUuid(s.getString("entity")));
                shop.open(s.getBoolean("open", true));
                shop.paymentOverride(s.getString("payment-override", "DEFAULT"));
                shop.paymentOverrideList(s.getStringList("payment-list"));
                shop.collect(readItems(s.getList("collect")));

                ConfigurationSection listings = s.getConfigurationSection("listings");
                if (listings != null) {
                    for (String listingKey : listings.getKeys(false)) {
                        UUID listingId = parseUuid(listingKey);
                        ConfigurationSection ls = listings.getConfigurationSection(listingKey);
                        ItemStack item = readItem(ls == null ? null : ls.get("item"));
                        if (listingId == null || ls == null || item == null) {
                            continue;
                        }
                        Listing listing = new Listing(listingId, item);
                        if (ls.isList("options")) {
                            for (Object raw : ls.getList("options")) {
                                if (!(raw instanceof ConfigurationSection option)
                                        || !(option.get("item") instanceof ItemStack paymentItem)) {
                                    continue;
                                }
                                listing.addOption(new PaymentOption(paymentItem, option.getInt("amount", 1)));
                            }
                        } else {
                            ConfigurationSection options = ls.getConfigurationSection("options");
                            if (options != null) {
                                for (String optionKey : options.getKeys(false)) {
                                    ConfigurationSection option = options.getConfigurationSection(optionKey);
                                    if (option != null && option.get("item") instanceof ItemStack paymentItem) {
                                        listing.addOption(new PaymentOption(paymentItem, option.getInt("amount", 1)));
                                    }
                                }
                            }
                        }
                        shop.listings().add(listing);
                    }
                }
                shops.put(id, shop);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Could not load shop " + key + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + shops.size() + " shop(s) from shops.yml");
    }

    /** Saves on the calling (main) thread; a tmp file prevents a half-written YAML on a crash. */
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("shops");
        for (Shop shop : shops.values()) {
            ConfigurationSection s = root.createSection(shop.id().toString());
            s.set("type", shop.isAdmin() ? "ADMIN" : "PLAYER");
            s.set("owner", shop.owner() == null ? "admin" : shop.owner().toString());
            s.set("name", shop.name());
            s.set("open", shop.open());
            s.set("payment-override", shop.paymentOverride());
            s.set("payment-list", shop.paymentOverrideList());
            if (shop.entityUuid() != null) {
                s.set("entity", shop.entityUuid().toString());
            }
            writeLocation(s, shop.location());
            ConfigurationSection listings = s.createSection("listings");
            for (Listing listing : shop.listings()) {
                ConfigurationSection ls = listings.createSection(listing.id().toString());
                ls.set("item", listing.item());
                ConfigurationSection options = ls.createSection("options");
                int index = 0;
                for (PaymentOption option : listing.options()) {
                    ConfigurationSection os = options.createSection(String.valueOf(index++));
                    os.set("item", option.item());
                    os.set("amount", option.amount());
                }
            }
            s.set("collect", shop.collect());
        }
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "shops.yml.tmp");
            yaml.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save shops.yml: " + ex.getMessage());
        }
    }

    private void writeLocation(ConfigurationSection s, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        ConfigurationSection l = s.createSection("location");
        l.set("world", location.getWorld().getUID().toString());
        l.set("x", location.getX());
        l.set("y", location.getY());
        l.set("z", location.getZ());
        l.set("yaw", location.getYaw());
        l.set("pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection l) {
        if (l == null) {
            return null;
        }
        World world = null;
        try {
            world = Bukkit.getWorld(UUID.fromString(l.getString("world", "")));
        } catch (IllegalArgumentException ignored) {
            // World removed from the server: keep the shop data so it can be fixed later.
        }
        if (world == null) {
            return null;
        }
        return new Location(world, l.getDouble("x"), l.getDouble("y"), l.getDouble("z"),
                (float) l.getDouble("yaw"), (float) l.getDouble("pitch"));
    }

    private ItemStack readItem(Object raw) {
        if (raw instanceof ItemStack item) {
            return item.clone();
        }
        return null;
    }

    private List<ItemStack> readItems(List<?> rawList) {
        List<ItemStack> out = new ArrayList<>();
        if (rawList == null) {
            return out;
        }
        for (Object raw : rawList) {
            ItemStack item = readItem(raw);
            if (item != null) {
                out.add(item);
            }
        }
        return out;
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
