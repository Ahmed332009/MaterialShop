package dev.materialshop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Append-only trade log with daily and size rotation. Every successful trade is logged. */
public final class TradeLogger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MaterialShop plugin;
    private final File directory;
    private final File current;
    private String activeDate = LocalDate.now().toString();

    public TradeLogger(MaterialShop plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "logs");
        this.current = new File(directory, "transactions.log");
    }

    public synchronized void log(String buyer, String seller, String get, String give, String location) {
        if (!plugin.settings().logEnabled()) {
            return;
        }
        try {
            directory.mkdirs();
            rotateIfNeeded();
            String line = LocalDateTime.now().format(TIME) + " | buyer=" + clean(buyer)
                    + " | seller=" + clean(seller) + " | bought=" + clean(get)
                    + " | paid=" + clean(give) + " | at=" + clean(location) + System.lineSeparator();
            Files.writeString(current.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write transaction log: " + ex.getMessage());
        }
    }

    private void rotateIfNeeded() throws IOException {
        String today = LocalDate.now().toString();
        boolean dayChanged = !today.equals(activeDate);
        long size = current.exists() ? Files.size(current.toPath()) : 0L;
        if (!dayChanged && size < plugin.settings().rotateBytes()) {
            return;
        }
        if (current.exists()) {
            String stamp = dayChanged ? activeDate : today;
            File target = new File(directory, "transactions-" + stamp + ".log");
            int suffix = 1;
            while (target.exists()) {
                target = new File(directory, "transactions-" + stamp + "-" + (suffix++) + ".log");
            }
            Files.move(current.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        activeDate = today;
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
            plugin.getLogger().warning("Could not read transaction log: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private String clean(String value) {
        return value == null ? "?" : value.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
