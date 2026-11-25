package com.banglaverse.materialshop.listener;

import com.banglaverse.materialshop.MaterialShop;
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

/** Chat prompts replace anvil GUIs and signs; callbacks always run on the main thread. */
public final class PromptManager implements Listener {
    private record Pending(Consumer<String> callback, BukkitTask timeout) {
    }

    private final MaterialShop plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PromptManager(MaterialShop plugin) {
        this.plugin = plugin;
    }

    /** Starts a prompt, replacing any older one from the same player. */
    public void request(Player player, Consumer<String> callback) {
        cancel(player.getUniqueId());
        UUID id = player.getUniqueId();
        Pending[] holder = new Pending[1];
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.get(id) == holder[0]) {
                pending.remove(id);
                if (player.isOnline()) {
                    plugin.msg().send(player, "prompt-timeout");
                }
            }
        }, plugin.settings().promptTicks());
        holder[0] = new Pending(callback, timeout);
        pending.put(id, holder[0]);
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (pending.get(id) != entry || !event.getPlayer().isOnline()) {
                return;
            }
            pending.remove(id);
            entry.timeout().cancel();
            if (text.equalsIgnoreCase("cancel")) {
                plugin.msg().send(event.getPlayer(), "prompt-cancelled");
                return;
            }
            entry.callback().accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }
}
