package dev.materialshop;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A shop: an infinite admin shop or a player shop backed by a physical chest. */
public final class Shop {
    public enum Type { ADMIN, PLAYER }

    private final UUID id;
    private final Type type;
    private final UUID owner;
    private String name;
    private Location location;
    private UUID entityUuid;
    private boolean open = true;
    private String paymentOverride = "DEFAULT";
    private final List<String> paymentOverrideList = new ArrayList<>();
    private final List<Listing> listings = new ArrayList<>();
    private final List<ItemStack> collect = new ArrayList<>();

    public Shop(UUID id, Type type, UUID owner, String name, Location location) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.name = name == null || name.isBlank() ? "Shop" : name.substring(0, Math.min(32, name.length()));
        this.location = location == null ? null : location.clone();
    }

    public UUID id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public boolean isAdmin() {
        return type == Type.ADMIN;
    }

    public UUID owner() {
        return owner;
    }

    public boolean ownedBy(UUID player) {
        return !isAdmin() && owner != null && owner.equals(player);
    }

    public String name() {
        return name;
    }

    public void name(String value) {
        this.name = value.substring(0, Math.min(32, value.length()));
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public void location(Location value) {
        this.location = value == null ? null : value.clone();
    }

    public UUID entityUuid() {
        return entityUuid;
    }

    public void entityUuid(UUID value) {
        this.entityUuid = value;
    }

    public boolean open() {
        return open;
    }

    public void open(boolean value) {
        this.open = value;
    }

    public String paymentOverride() {
        return paymentOverride;
    }

    public void paymentOverride(String value) {
        this.paymentOverride = value == null ? "DEFAULT" : value.toUpperCase(java.util.Locale.ROOT);
    }

    public List<String> paymentOverrideList() {
        return new ArrayList<>(paymentOverrideList);
    }

    public void paymentOverrideList(List<String> values) {
        paymentOverrideList.clear();
        if (values != null) {
            paymentOverrideList.addAll(values);
        }
    }

    public List<Listing> listings() {
        return listings;
    }

    public Listing listing(UUID listingId) {
        for (Listing listing : listings) {
            if (listing.id().equals(listingId)) {
                return listing;
            }
        }
        return null;
    }

    public void removeListing(UUID listingId) {
        listings.removeIf(listing -> listing.id().equals(listingId));
    }

    /** Deep copy: callers must never hold the shop's live ItemStacks. */
    public List<ItemStack> collect() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack item : collect) {
            out.add(item.clone());
        }
        return out;
    }

    public void collect(List<ItemStack> items) {
        collect.clear();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    collect.add(item.clone());
                }
            }
        }
    }
}
