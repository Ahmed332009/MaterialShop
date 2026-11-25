package com.banglaverse.materialshop.command;

import com.banglaverse.materialshop.Items;
import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.menu.SetupMenu;
import com.banglaverse.materialshop.menu.ShopViewMenu;
import com.banglaverse.materialshop.model.Listing;
import com.banglaverse.materialshop.model.Shop;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Staff commands for server shops, NPC tags, reload and logs. */
public final class ShopAdminCommand implements CommandExecutor, TabCompleter {
    private final MaterialShop plugin;

    public ShopAdminCommand(MaterialShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("materialshop.admin")) {
            plugin.msg().send(sender, "no-permission-admin");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.msg().send(sender, "player-only");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> help(player);
            case "create" -> create(player, args);
            case "remove" -> remove(player, args);
            case "edit" -> edit(player, args);
            case "npc" -> npc(player, args);
            case "list" -> list(player);
            case "reload" -> {
                plugin.reloadAll();
                plugin.msg().send(player, "reloaded");
            }
            case "log" -> {
                List<String> lines = plugin.logs().recent(plugin.settings().logLines());
                if (lines.isEmpty()) {
                    plugin.msg().send(player, "trade-log-empty");
                } else {
                    for (String line : lines) {
                        plugin.msg().sendPlain(player, "trade-log-line", "line", plugin.msg().clean(line));
                    }
                }
            }
            default -> plugin.msg().send(player, "unknown-command", "command", "/shopadmin");
        }
        return true;
    }

    private void help(Player player) {
        plugin.msg().send(player, "help-header");
        for (String key : List.of("help-admin-1", "help-admin-2", "help-admin-3", "help-admin-4",
                "help-admin-5", "help-admin-6", "help-admin-7")) {
            plugin.msg().send(player, key);
        }
    }

    private void create(Player player, String[] args) {
        String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Server shop";
        Shop shop = new Shop(UUID.randomUUID(), Shop.Type.SERVER, null, name, player.getLocation());
        plugin.data().saveShop(shop);
        plugin.msg().send(player, "server-shop-created", "name", plugin.msg().clean(name),
                "id", shop.id().toString());
        new SetupMenu(plugin, player, SetupMenu.Mode.SERVER, shop.id(), null, name, null).open();
    }

    private void remove(Player player, String[] args) {
        Shop shop = args.length > 1 ? plugin.data().shop(uuid(args[1])) : null;
        if (shop == null || !shop.isServer()) {
            plugin.msg().send(player, "server-shop-not-found");
            return;
        }
        for (Listing listing : plugin.data().listingsByShop(shop.id())) {
            plugin.data().removeListing(listing.id());
        }
        plugin.data().removeShop(shop.id());
        plugin.msg().send(player, "server-shop-removed");
    }

    private void edit(Player player, String[] args) {
        Shop shop = args.length > 1 ? plugin.data().shop(uuid(args[1])) : null;
        if (shop == null || !shop.isServer()) {
            plugin.msg().send(player, "server-shop-not-found");
            return;
        }
        new ShopViewMenu(plugin, player, shop.id(), true).open();
    }

    private void npc(Player player, String[] args) {
        if (!(player.getTargetEntity(6) instanceof Villager villager)) {
            plugin.msg().send(player, "npc-need-villager");
            return;
        }
        Shop shop = args.length > 1 ? plugin.data().shop(uuid(args[1])) : null;
        if (shop == null || !shop.isServer()) {
            plugin.msg().send(player, "npc-need-id");
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "shop");
        villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, shop.id().toString());
        villager.setPersistent(true);
        plugin.msg().send(player, "npc-linked");
    }

    private void list(Player player) {
        plugin.msg().send(player, "shop-list-header");
        for (Shop shop : plugin.data().shops(Shop.Type.SERVER)) {
            plugin.msg().sendPlain(player, "shop-list-line", "id", shop.id().toString(),
                    "name", plugin.msg().clean(shop.name()),
                    "count", String.valueOf(plugin.data().listingsByShop(shop.id()).size()));
        }
    }

    private UUID uuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("materialshop.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String sub : List.of("create", "remove", "edit", "npc", "list", "reload", "log")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && List.of("remove", "edit", "npc").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Shop shop : plugin.data().shops(Shop.Type.SERVER)) {
                if (shop.id().toString().startsWith(args[1])) {
                    out.add(shop.id().toString());
                }
            }
        }
        return out;
    }
}
