package com.banglaverse.materialshop.trade;

import com.banglaverse.materialshop.MaterialShop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Size-rotated transaction log at logs/transactions.log. */
public final class TradeLogger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MaterialShop plugin;
    private final File directory;
    private final File current;

    public TradeLogger(MaterialShop plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "logs");
        this.current = new File(directory, "transactions.log");
    }

    public synchronized void log(String buyer, String seller, String item, String payment, String listingId) {
        if (!plugin.settings().logEnabled()) {
            return;
        }
        try {
            directory.mkdirs();
            if (current.exists() && Files.size(current.toPath()) >= plugin.settings().rotateBytes()) {
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                File target = new File(directory, "transactions-" + stamp + ".log");
                int suffix = 1;
                while (target.exists()) {
                    target = new File(directory, "transactions-" + stamp + "-" + (suffix++) + ".log");
                }
                Files.move(current.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            String line = LocalDateTime.now().format(TIME) + " | buyer=" + clean(buyer)
                    + " | seller=" + clean(seller) + " | item=" + clean(item)
                    + " | payment=" + clean(payment) + " | listing=" + clean(listingId)
                    + System.lineSeparator();
            Files.writeString(current.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write transaction log: " + ex.getMessage());
        }
    }

    public synchronized List<String> recent(int count) {
        if (!current.exists()) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = Files.readAllLines(current.toPath(), StandardCharsets.UTF_8);
            Collections.reverse(lines);
            return lines.subList(0, Math.min(count, lines.size()));
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    private String clean(String value) {
        return value == null ? "?" : value.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
