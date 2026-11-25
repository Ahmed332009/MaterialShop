package com.banglaverse.materialshop.menu;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared chest-menu safety and helpers for every screen. */
public abstract class MenuBase implements InventoryHolder {
    protected final MaterialShop plugin;
    protected final Player player;
    protected final Map<Integer, Runnable> actions = new HashMap<>();
    protected Inventory inv;

    protected MenuBase(MaterialShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract int size();

    protected abstract String titleKey();

    protected String[] titleArgs() {
        return new String[0];
    }

    protected abstract void build();

    /** Creates and opens this menu. */
    public void open() {
        inv = Bukkit.createInventory(this, size(), plugin.msg().plain(titleKey(), titleArgs()));
        rebuild();
        player.openInventory(inv);
    }

    /** Rebuilds the same inventory without a close event. */
    protected void rebuild() {
        actions.clear();
        inv.clear();
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(plugin.msg().plain("menu.common.filler"));
        filler.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        build();
    }

    protected void set(int slot, ItemStack item, Runnable action) {
        inv.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected ItemStack icon(Material material, String key, String... kv) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.msg().plain(key, kv));
        meta.lore(plugin.msg().lore(key + "-lore", kv));
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack itemIcon(ItemStack source, String key, List<Component> extra, String... kv) {
        ItemStack item = source.clone();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.msg().plain(key, kv));
        List<Component> lore = new ArrayList<>(plugin.msg().lore(key + "-lore", kv));
        lore.addAll(extra);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    protected List<Component> line(String key, String... kv) {
        return List.of(plugin.msg().plain(key, kv));
    }

    protected void backButton(int slot, com.banglaverse.materialshop.menu.MainMenu destination) {
        set(slot, icon(Material.BARRIER, "menu.common.back"), destination::open);
    }

    protected void backToMain(int slot) {
        set(slot, icon(Material.BARRIER, "menu.common.back"),
                () -> new com.banglaverse.materialshop.menu.MainMenu(plugin, player).open());
    }

    /** Cancels shift-click, hotkeys, double-click collect and offhand swaps. */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClick().name().startsWith("SHIFT")
                || event.getClick().name().equals("NUMBER_KEY")
                || event.getClick().name().equals("DOUBLE_CLICK")) {
            return;
        }
        Runnable action = actions.get(event.getRawSlot());
        if (action != null) {
            action.run();
        }
    }

    /** Drags are cancelled everywhere; setup menus still work with taps. */
    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /** Called when the inventory closes; setup menus use it to return placed items. */
    public void onClose() {
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    protected UUID uuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    protected String itemName(ItemStack item) {
        return plugin.msg().clean(Items.name(item));
    }
}
