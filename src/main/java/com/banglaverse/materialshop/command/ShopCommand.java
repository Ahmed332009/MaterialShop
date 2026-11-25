package com.banglaverse.materialshop.command;

import com.banglaverse.materialshop.MaterialShop;
import com.banglaverse.materialshop.hook.ClaimHook;
import com.banglaverse.materialshop.menu.CollectMenu;
import com.banglaverse.materialshop.menu.MainMenu;
import com.banglaverse.materialshop.menu.MyListingsMenu;
import com.banglaverse.materialshop.menu.SetupMenu;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Handles /shop, /ms and /materialshop. */
public final class ShopCommand implements CommandExecutor, TabCompleter {
    private final MaterialShop plugin;

    public ShopCommand(MaterialShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg().send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("materialshop.use")) {
            plugin.msg().send(player, "no-permission");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> new MainMenu(plugin, player).open();
            case "list" -> new MyListingsMenu(plugin, player).open();
            case "collect" -> {
                if (player.hasPermission("materialshop.collect")) {
                    new CollectMenu(plugin, player).open();
                } else {
                    plugin.msg().send(player, "no-permission");
                }
            }
            case "help" -> help(player);
            case "chest" -> chest(player, args);
            default -> plugin.msg().send(player, "unknown-command", "command", "/shop");
        }
        return true;
    }

    private void help(Player player) {
        for (String key : List.of("help-header", "help-1", "help-2", "help-3", "help-4", "help-5")) {
            plugin.msg().send(player, key);
        }
        if (player.hasPermission("materialshop.admin")) {
            for (String key : List.of("help-admin-1", "help-admin-2", "help-admin-3",
                    "help-admin-4", "help-admin-5", "help-admin-6", "help-admin-7")) {
                plugin.msg().send(player, key);
            }
        }
    }

    private void chest(Player player, String[] args) {
        if (!plugin.settings().chestEnabled()) {
            plugin.msg().send(player, "chest-shops-disabled");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("create")) {
            plugin.msg().send(player, "unknown-command", "command", "/shop chest create");
            return;
        }
        if (!player.hasPermission("materialshop.create")) {
            plugin.msg().send(player, "no-permission-create");
            return;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || !(block.getState() instanceof Container)) {
            plugin.msg().send(player, "need-chest");
            return;
        }
        if (plugin.data().chestShopCount(player.getUniqueId()) >= plugin.settings().shopLimit(player)) {
            plugin.msg().send(player, "shop-limit-reached", "limit",
                    String.valueOf(plugin.settings().shopLimit(player)));
            return;
        }
        if (!new ClaimHook(plugin).canModify(player, block)) {
            plugin.msg().send(player, "claim-denied");
            return;
        }
        String name = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : player.getName() + "'s shop";
        new SetupMenu(plugin, player, SetupMenu.Mode.CHEST, null, block.getLocation(), name, null).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player) || !player.hasPermission("materialshop.use")) {
            return out;
        }
        if (args.length == 1) {
            for (String sub : List.of("list", "collect", "help", "chest")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chest")) {
            if ("create".startsWith(args[1].toLowerCase(Locale.ROOT)) && plugin.settings().chestEnabled()) {
                out.add("create");
            }
        }
        return out;
    }
}
