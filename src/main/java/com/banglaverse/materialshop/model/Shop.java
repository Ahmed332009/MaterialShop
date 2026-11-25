package com.banglaverse.materialshop.model;

import org.bukkit.Location;

import java.util.UUID;

/** A server shop with infinite stock or a player-owned chest shop. */
public final class Shop {
    public enum Type { SERVER, CHEST }

    private final UUID id;
    private final Type type;
    private final UUID owner;
    private String name;
    private Location location;

    public Shop(UUID id, Type type, UUID owner, String name, Location location) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.name = name == null ? "Shop" : name.substring(0, Math.min(32, name.length()));
        this.location = location == null ? null : location.clone();
    }

    public UUID id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public boolean isServer() {
        return type == Type.SERVER;
    }

    public UUID owner() {
        return owner;
    }

    public boolean ownedBy(UUID player) {
        return !isServer() && owner != null && owner.equals(player);
    }

    public String name() {
        return name;
    }

    public void name(String value) {
        name = value.substring(0, Math.min(32, value.length()));
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public void location(Location value) {
        location = value == null ? null : value.clone();
    }
}
