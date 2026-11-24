package dev.materialshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /shop commands plus the "place a chest to create" flow. */
public final class ShopCommand implements CommandExecutor, TabCompleter, Listener {
    private final MaterialShop plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    private record Pending(ItemStack item, String name, BukkitTask timeout) {
    }

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
        String sub = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "", "menu" -> plugin.gui().openMain(player);
            case "help" -> {
                for (String key : List.of("help-header", "help-1", "help-2", "help-3", "help-4", "help-5")) {
                    plugin.msg().send(player, key);
                }
                if (player.hasPermission("materialshop.admin")) {
                    for (String key : List.of("help-admin-1", "help-admin-2", "help-admin-3",
                            "help-admin-4", "help-admin-5", "help-admin-6")) {
                        plugin.msg().send(player, key);
                    }
                }
            }
            case "list" -> plugin.gui().openList(player, "MINE", 0);
            case "collect" -> {
                if (!player.hasPermission("materialshop.collect")) {
                    plugin.msg().send(player, "no-permission");
                    return true;
                }
                plugin.gui().openCollect(player, false);
            }
            case "create" -> create(player, join(args));
            default -> plugin.msg().send(player, "unknown-command", "command", "/shop");
        }
        return true;
    }

    private String join(String[] args) {
        if (args.length <= 1) {
            return "";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    }

    private void create(Player player, String rawName) {
        if (!player.hasPermission("materialshop.create")) {
            plugin.msg().send(player, "no-permission");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            plugin.msg().send(player, "create-no-item");
            return;
        }
        String name = rawName.isBlank() ? player.getName() + "'s shop" : rawName;
        Block target = player.getTargetBlockExact(5);
        if (target != null && target.getState() instanceof Chest) {
            Shop shop = plugin.shops().createPlayer(player, target, held, name);
            if (shop != null) {
                plugin.gui().openEditor(player, shop);
            }
            return;
        }
        cancel(player.getUniqueId());
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(player.getUniqueId()) != null) {
                plugin.msg().send(player, "create-timeout");
            }
        }, plugin.settings().promptTicks());
        pending.put(player.getUniqueId(), new Pending(held.clone(), name, timeout));
        plugin.msg().send(player, "create-not-chest");
        plugin.msg().send(player, "create-placement-hint", "name", plugin.msg().escape(name));
        plugin.msg().send(player, "prompt-cancel-hint");
    }

    private void cancel(UUID id) {
        Pending old = pending.remove(id);
        if (old != null && old.timeout() != null) {
            old.timeout().cancel();
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Pending creation = pending.get(event.getPlayer().getUniqueId());
        if (creation == null || !(event.getBlock().getState() instanceof Chest)) {
            return;
        }
        cancel(event.getPlayer().getUniqueId());
        Shop shop = plugin.shops().createPlayer(event.getPlayer(), event.getBlock(),
                creation.item(), creation.name());
        if (shop != null) {
            plugin.gui().openEditor(event.getPlayer(), shop);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!pending.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (!text.equalsIgnoreCase("cancel")) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (cancelPending(event.getPlayer().getUniqueId())) {
                plugin.msg().send(event.getPlayer(), "create-cancelled");
            }
        });
    }

    private boolean cancelPending(UUID id) {
        if (!pending.containsKey(id)) {
            return false;
        }
        cancel(id);
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player) || !player.hasPermission("materialshop.use")) {
            return out;
        }
        if (args.length == 1) {
            for (String sub : List.of("create", "list", "collect", "help")) {
                if (sub.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) {
                    out.add(sub);
                }
            }
        }
        return out;
    }
}
