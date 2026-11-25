package com.banglaverse.materialshop.storage;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.model.CollectBox;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.PaymentOption;
import com.banglaverse.materialshop.model.Shop;
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

/** Atomic YAML store for listings, escrow, collect boxes and shops. */
public final class DataStore {
    private final MaterialShop plugin;
    private final File file;
    private final Map<UUID, Listing> listings = new LinkedHashMap<>();
    private final Map<UUID, CollectBox> boxes = new LinkedHashMap<>();
    private final Map<UUID, Shop> shops = new LinkedHashMap<>();

    public DataStore(MaterialShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public Map<UUID, Listing> listings() {
        return listings;
    }

    public Map<UUID, Shop> shops() {
        return shops;
    }

    public Listing listing(UUID id) {
        return listings.get(id);
    }

    public void saveListing(Listing listing) {
        listings.put(listing.id(), listing);
        save();
    }

    public void removeListing(UUID id) {
        listings.remove(id);
        save();
    }

    public Shop shop(UUID id) {
        return shops.get(id);
    }

    public void saveShop(Shop shop) {
        shops.put(shop.id(), shop);
        save();
    }

    public void removeShop(UUID id) {
        shops.remove(id);
        save();
    }

    public CollectBox box(UUID owner) {
        return boxes.computeIfAbsent(owner, ignored -> new CollectBox());
    }

    public List<Listing> listingsByOwner(UUID owner) {
        List<Listing> out = new ArrayList<>();
        for (Listing listing : listings.values()) {
            if (listing.owner().equals(owner)) {
                out.add(listing);
            }
        }
        return out;
    }

    public List<Listing> listingsByShop(UUID shopId) {
        List<Listing> out = new ArrayList<>();
        for (Listing listing : listings.values()) {
            if (listing.shopId() != null && listing.shopId().equals(shopId)) {
                out.add(listing);
            }
        }
        return out;
    }

    public List<Shop> shops(Shop.Type type) {
        List<Shop> out = new ArrayList<>();
        for (Shop shop : shops.values()) {
            if (shop.type() == type) {
                out.add(shop);
            }
        }
        return out;
    }

    public int chestShopCount(UUID owner) {
        int count = 0;
        for (Shop shop : shops.values()) {
            if (shop.ownedBy(owner)) {
                count++;
            }
        }
        return count;
    }

    /** Loads data.yml. Missing files simply start empty. */
    public void load() {
        listings.clear();
        boxes.clear();
        shops.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection shopSec = yaml.getConfigurationSection("shops");
        if (shopSec != null) {
            for (String key : shopSec.getKeys(false)) {
                UUID id = uuid(key);
                ConfigurationSection s = shopSec.getConfigurationSection(key);
                if (id == null || s == null) {
                    continue;
                }
                Shop.Type type = "CHEST".equalsIgnoreCase(s.getString("type", "SERVER"))
                        ? Shop.Type.CHEST : Shop.Type.SERVER;
                shops.put(id, new Shop(id, type, uuid(s.getString("owner")),
                        s.getString("name", "Shop"), readLocation(s.getConfigurationSection("location"))));
            }
        }

        ConfigurationSection listingSec = yaml.getConfigurationSection("listings");
        if (listingSec != null) {
            for (String key : listingSec.getKeys(false)) {
                UUID id = uuid(key);
                ConfigurationSection s = listingSec.getConfigurationSection(key);
                ItemStack item = s == null ? null : item(s.get("item"));
                if (id == null || s == null || item == null) {
                    continue;
                }
                Listing.Source source;
                try {
                    source = Listing.Source.valueOf(s.getString("source", "PLAYER").toUpperCase());
                } catch (IllegalArgumentException ex) {
                    source = Listing.Source.PLAYER;
                }
                Listing listing = new Listing(id, uuid(s.getString("owner")), source,
                        uuid(s.getString("shop")), item, 0);
                listing.expiresAt(s.getLong("expires-at", 0));
                listing.options(readOptions(s.getConfigurationSection("options")));
                listing.escrow(readItems(s.getList("escrow")));
                listings.put(id, listing);
            }
        }

        ConfigurationSection boxSec = yaml.getConfigurationSection("collect");
        if (boxSec != null) {
            for (String key : boxSec.getKeys(false)) {
                UUID owner = uuid(key);
                ConfigurationSection s = boxSec.getConfigurationSection(key);
                if (owner != null && s != null) {
                    CollectBox box = new CollectBox();
                    box.stacks(readItems(s.getList("stacks")));
                    boxes.put(owner, box);
                }
            }
        }
        plugin.getLogger().info("MaterialShop data loaded: " + listings.size() + " listing(s), "
                + shops.size() + " shop(s).");
    }

    /** Writes a tmp file first, then moves it into place. */
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection shopSec = yaml.createSection("shops");
        for (Shop shop : shops.values()) {
            ConfigurationSection s = shopSec.createSection(shop.id().toString());
            s.set("type", shop.isServer() ? "SERVER" : "CHEST");
            s.set("owner", shop.owner() == null ? "server" : shop.owner().toString());
            s.set("name", shop.name());
            writeLocation(s, shop.location());
        }
        ConfigurationSection listingSec = yaml.createSection("listings");
        for (Listing listing : listings.values()) {
            ConfigurationSection s = listingSec.createSection(listing.id().toString());
            s.set("source", listing.source().name());
            s.set("owner", listing.owner() == null ? "server" : listing.owner().toString());
            s.set("shop", listing.shopId() == null ? "" : listing.shopId().toString());
            s.set("item", listing.item());
            s.set("created", listing.created());
            s.set("expires-at", listing.expiresAt());
            s.set("escrow", listing.escrow());
            ConfigurationSection options = s.createSection("options");
            int index = 0;
            for (PaymentOption option : listing.options()) {
                ConfigurationSection os = options.createSection(String.valueOf(index++));
                os.set("item", option.item());
            }
        }
        ConfigurationSection boxSec = yaml.createSection("collect");
        for (Map.Entry<UUID, CollectBox> entry : boxes.entrySet()) {
            boxSec.createSection(entry.getKey().toString()).set("stacks", entry.getValue().stacks());
        }
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "data.yml.tmp");
            yaml.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException fallback) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }

    private List<PaymentOption> readOptions(ConfigurationSection options) {
        List<PaymentOption> out = new ArrayList<>();
        if (options == null) {
            return out;
        }
        for (String key : options.getKeys(false)) {
            ConfigurationSection os = options.getConfigurationSection(key);
            ItemStack item = os == null ? null : item(os.get("item"));
            if (item != null) {
                out.add(new PaymentOption(item));
            }
        }
        return out;
    }

    private List<ItemStack> readItems(List<?> values) {
        List<ItemStack> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (Object value : values) {
            ItemStack item = item(value);
            if (item != null) {
                out.add(item);
            }
        }
        return out;
    }

    private ItemStack item(Object raw) {
        return raw instanceof ItemStack item ? item.clone() : null;
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
    }

    private Location readLocation(ConfigurationSection l) {
        if (l == null) {
            return null;
        }
        try {
            World world = Bukkit.getWorld(UUID.fromString(l.getString("world", "")));
            return world == null ? null : new Location(world, l.getDouble("x"), l.getDouble("y"), l.getDouble("z"));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private UUID uuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
