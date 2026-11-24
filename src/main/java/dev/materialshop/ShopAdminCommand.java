package dev.materialshop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /shopadmin, /materialshop and /ms: admin shop lifecycle, reload and log. */
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
            case "", "help" -> {
                plugin.msg().send(player, "help-header");
                for (String key : List.of("help-admin-1", "help-admin-2", "help-admin-3",
                        "help-admin-4", "help-admin-5", "help-admin-6")) {
                    plugin.msg().send(player, key);
                }
            }
            case "create" -> create(player, join(args));
            case "edit" -> {
                Shop shop = args.length > 1 ? plugin.shops().get(parse(args[1])) : targeted(player);
                if (shop == null) {
                    plugin.msg().send(player, "admin-not-found");
                    return true;
                }
                plugin.gui().openEditor(player, shop);
            }
            case "remove" -> {
                if (args.length < 2) {
                    plugin.msg().send(player, "admin-not-found");
                    return true;
                }
                Shop shop = plugin.shops().get(parse(args[1]));
                if (shop == null || !shop.isAdmin()) {
                    plugin.msg().send(player, "admin-not-found");
                    return true;
                }
                refundCollect(player, shop);
                plugin.storage().remove(shop.id());
                plugin.msg().send(player, "admin-removed", "name", plugin.msg().escape(shop.name()));
            }
            case "reload" -> {
                plugin.reloadAll();
                plugin.msg().send(player, "reloaded");
            }
            case "log" -> plugin.gui().openLog(player);
            case "collect" -> plugin.gui().openCollect(player, true);
            default -> plugin.msg().send(player, "unknown-command", "command", "/shopadmin");
        }
        return true;
    }

    private String join(String[] args) {
        return args.length <= 1 ? "" : String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    }

    private void create(Player player, String rawName) {
        String name = rawName.isBlank() ? "Admin Shop" : rawName;
        Block target = player.getTargetBlockExact(5);
        Location at = target == null ? player.getLocation() : target.getLocation();
        Shop shop = plugin.shops().createAdmin(name, at);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null && !held.getType().isAir()) {
            shop.listings().add(new Listing(java.util.UUID.randomUUID(), held));
            plugin.shops().save(shop);
        }
        plugin.msg().send(player, "admin-created", "name", plugin.msg().escape(shop.name()),
                "id", shop.id().toString());
        plugin.gui().openEditor(player, shop);
    }

    private Shop targeted(Player player) {
        Entity entity = player.getTargetEntity(6);
        if (entity != null) {
            for (Shop shop : plugin.shops().all()) {
                if (entity.getUniqueId().equals(shop.entityUuid())) {
                    return shop;
                }
            }
        }
        Block block = player.getTargetBlockExact(5);
        if (block != null) {
            for (Shop shop : plugin.shops().all()) {
                if (shop.location() != null && shop.location().getBlock().equals(block)) {
                    return shop;
                }
            }
        }
        return null;
    }

    private void refundCollect(Player player, Shop shop) {
        for (ItemStack item : shop.collect()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack rest : leftover.values()) {
                if (shop.location() != null && shop.location().getWorld() != null) {
                    shop.location().getWorld().dropItemNaturally(shop.location(), rest);
                    plugin.msg().send(player, "admin-collect-overflow", "item",
                            Items.describe(rest, rest.getAmount()));
                }
            }
        }
    }

    private java.util.UUID parse(String raw) {
        try {
            return java.util.UUID.fromString(raw);
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
            for (String sub : List.of("create", "remove", "edit", "reload", "log", "collect")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("remove"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Shop shop : plugin.shops().all()) {
                if (shop.id().toString().startsWith(prefix)) {
                    out.add(shop.id().toString());
                }
            }
        }
        return out;
    }
}
