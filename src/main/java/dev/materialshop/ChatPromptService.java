package dev.materialshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Chat prompts instead of anvil GUIs and signs: the next chat line is captured and
 * handled on the main thread. Typing "cancel" or letting the timer expire aborts it.
 */
public final class ChatPromptService implements Listener {
    public enum Kind { CUSTOM_QUANTITY, PAYMENT_AMOUNT, SHOP_NAME }

    private record Pending(UUID id, Kind kind, Consumer<String> callback, BukkitTask timeout) {
    }

    private final MaterialShop plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPromptService(MaterialShop plugin) {
        this.plugin = plugin;
    }

    public boolean has(UUID id) {
        return pending.containsKey(id);
    }

    /** Replaces any older prompt. The callback always runs on the main server thread. */
    public void request(Player player, Kind kind, Consumer<String> callback) {
        cancel(player.getUniqueId());
        UUID id = player.getUniqueId();
        Pending[] holder = new Pending[1];
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending current = pending.get(id);
            if (current != null && current == holder[0]) {
                pending.remove(id);
                if (player.isOnline()) {
                    plugin.msg().send(player, "prompt-timeout");
                }
            }
        }, plugin.settings().promptTicks());
        Pending entry = new Pending(id, kind, callback, timeout);
        holder[0] = entry;
        pending.put(id, entry);
        plugin.msg().send(player, "prompt-cancel-hint");
    }

    public void cancel(UUID id) {
        Pending old = pending.remove(id);
        if (old != null && old.timeout() != null) {
            old.timeout().cancel();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Pending entry = pending.get(id);
        if (entry == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        // Inventories and shop data may only be touched on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Pending current = pending.get(id);
            if (current == null || current != entry || !event.getPlayer().isOnline()) {
                return;
            }
            pending.remove(id);
            current.timeout().cancel();
            if (text.equalsIgnoreCase("cancel")) {
                plugin.msg().send(event.getPlayer(), "prompt-cancelled");
                return;
            }
            current.callback().accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }
}
