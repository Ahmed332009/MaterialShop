package dev.materialshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** All menus are plain chest inventories: one tap works for Java and Bedrock/Geyser. */
public final class GuiManager implements Listener {
    public enum Screen { MAIN, LIST, SHOP, PURCHASE, EDIT, COLLECT, LOG }

    /** Holder carrying menu state. Actions are short strings, e.g. "qty:8". */
    public static final class Menu implements InventoryHolder {
        final Screen screen;
        UUID shopId;
        UUID listingId;
        String listType;
        int page;
        int option;
        int quantity = 1;
        boolean adminCollect;
        final Map<Integer, String> actions = new HashMap<>();
        Inventory inv;

        Menu(Screen screen) {
            this.screen = screen;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final MaterialShop plugin;
    private final Map<UUID, UUID> selectedListing = new HashMap<>();
    private final Map<UUID, Integer> selectedOption = new HashMap<>();

    public GuiManager(MaterialShop plugin) {
        this.plugin = plugin;
    }

    private Component t(String key, String... kv) {
        return plugin.msg().plain("gui." + key, kv).decoration(TextDecoration.ITALIC, false);
    }

    private Component plain(String key, String... kv) {
        return plugin.msg().get(key, kv);
    }

    private String esc(String value) {
        return plugin.msg().escape(value);
    }

    private ItemStack icon(Material material, String key, String... kv) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(t(key, kv));
        List<Component> lore = plugin.msg().lore("gui." + key + "-lore", kv);
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack itemIcon(ItemStack source, String nameKey, List<Component> extraLore, String... kv) {
        ItemStack item = source.clone();
        item.setAmount(Math.max(1, item.getAmount()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(t(nameKey, kv));
        List<Component> lore = new ArrayList<>();
        lore.addAll(plugin.msg().lore("gui." + nameKey + "-lore", kv).stream()
                .map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        lore.addAll(extraLore);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(t("unfilled").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Menu create(Player player, Screen screen, int rows, String titleKey, String... kv) {
        Menu menu = new Menu(screen);
        menu.inv = Bukkit.createInventory(menu, rows * 9,
                t(titleKey, kv).decoration(TextDecoration.ITALIC, false));
        for (int i = 0; i < menu.inv.getSize(); i++) {
            menu.inv.setItem(i, pane());
        }
        return menu;
    }

    private void set(Menu menu, int slot, ItemStack item, String action) {
        menu.inv.setItem(slot, item);
        if (action != null) {
            menu.actions.put(slot, action);
        }
    }

    private void show(Player player, Menu menu) {
        player.openInventory(menu.inv);
    }

    private UUID parse(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------- main menu

    public void openMain(Player player) {
        Menu menu = create(player, Screen.MAIN, 3, "main-title");
        set(menu, 10, icon(Material.GOLD_INGOT, "admin-browse"), "browse:ADMIN:0");
        set(menu, 12, icon(Material.CHEST, "player-browse"), "browse:PLAYER:0");
        set(menu, 14, icon(Material.BOOK, "my-shops", "count",
                String.valueOf(plugin.shops().ownedBy(player.getUniqueId()).size())), "browse:MINE:0");
        set(menu, 16, icon(Material.HOPPER, "collect"), "collect:player");

        int slot = 28;
        if (player.hasPermission("materialshop.admin")) {
            set(menu, slot++, icon(Material.DIAMOND, "admin-collect"), "collect:admin");
            set(menu, slot++, icon(Material.PAPER, "log"), "log");
        }
        set(menu, 26, icon(Material.BARRIER, "close"), "close");
        show(player, menu);
    }

    // --------------------------------------------------------------- shop listings

    public void openList(Player player, String type, int page) {
        List<Shop> shops = switch (type) {
            case "ADMIN" -> plugin.shops().adminShops();
            case "MINE" -> plugin.shops().ownedBy(player.getUniqueId());
            default -> plugin.shops().playerShops();
        };
        shops.sort(Comparator.comparing(Shop::name));
        Menu menu = create(player, Screen.LIST, 6, "list-title", "page", String.valueOf(page + 1),
                "pages", String.valueOf(Math.max(1, (shops.size() + SLOTS.length - 1) / SLOTS.length)));
        menu.listType = type;
        menu.page = page;
        paginate(menu, shops.size(), page, "browse:" + type, "main");
        for (int i = 0; i < Math.min(SLOTS.length, Math.max(0, shops.size() - page * SLOTS.length)); i++) {
            Shop shop = shops.get(page * SLOTS.length + i);
            String typeName = plugin.msg().string(shop.isAdmin() ? "word-admin" : "word-player",
                    shop.isAdmin() ? "Admin" : "Player");
            List<Component> lore = plugin.msg().lore("gui.shop-entry-lore",
                    "type", esc(typeName),
                    "listings", String.valueOf(shop.listings().size()),
                    "owner", esc(plugin.shops().ownerName(shop)));
            ItemStack item = itemIcon(new ItemStack(shop.isAdmin() ? Material.GOLD_BLOCK : Material.CHEST),
                    "shop-entry-name", lore, "name", esc(shop.name()));
            set(menu, SLOTS[i], item, "shop:" + shop.id() + ":0");
        }
        if (shops.isEmpty()) {
            set(menu, 22, icon(Material.BARRIER, "empty"), null);
        }
        show(player, menu);
    }

    private void paginate(Menu menu, int total, int page, String actionPrefix, String backAction) {
        int pages = Math.max(1, (total + SLOTS.length - 1) / SLOTS.length);
        menu.page = Math.max(0, Math.min(page, pages - 1));
        set(menu, 48, icon(Material.ARROW, "previous"), actionPrefix + ":" + (menu.page - 1));
        set(menu, 49, icon(Material.PAPER, "page", "page", String.valueOf(menu.page + 1),
                "pages", String.valueOf(pages)), null);
        set(menu, 50, icon(Material.ARROW, "next"), actionPrefix + ":" + (menu.page + 1));
        set(menu, 53, icon(Material.BARRIER, "back"), backAction);
    }

    // -------------------------------------------------------------------- shop view

    public void openShop(Player player, Shop shop, int page) {
        if (shop == null) {
            plugin.msg().send(player, "admin-not-found");
            return;
        }
        Menu menu = create(player, Screen.SHOP, 6, "shop-title", "name", esc(shop.name()));
        menu.shopId = shop.id();
        menu.page = page;
        int listings = shop.listings().size();
        int pages = Math.max(1, (listings + SLOTS.length - 1) / SLOTS.length);
        paginate(menu, listings, page, "shop:" + shop.id(), "main");

        for (int i = 0; i < Math.min(SLOTS.length, Math.max(0, listings - page * SLOTS.length)); i++) {
            Listing listing = shop.listings().get(page * SLOTS.length + i);
            String stock = shop.isAdmin() ? plugin.msg().string("word-infinite", "infinite")
                    : String.valueOf(plugin.shops().stock(shop, listing));
            List<Component> lore = new ArrayList<>(plugin.msg().lore("gui.listing-lore",
                    "stock", esc(stock)));
            lore.add(t("payment-header"));
            for (PaymentOption option : listing.options()) {
                lore.add(t("payment-option", "amount", String.valueOf(option.amount()),
                        "item", esc(Items.name(option.item()))));
            }
            ItemStack display = itemIcon(listing.item(), "listing-name", lore,
                    "item", esc(Items.name(listing.item())));
            set(menu, SLOTS[i], display, "listing:" + listing.id());
        }
        if (listings == 0) {
            set(menu, 22, icon(Material.BARRIER, "empty"), null);
        }
        set(menu, 47, itemIcon(new ItemStack(Material.PLAYER_HEAD), "shop-entry-name",
                List.of(plain("gui.owner-line", "owner", esc(plugin.shops().ownerName(shop)))),
                "name", esc(plugin.shops().ownerName(shop))));
        if (plugin.shops().mayManage(player, shop)) {
            set(menu, 45, icon(Material.ANVIL, "edit"), "edit:open:" + shop.id());
        }
        show(player, menu);
    }

    // ------------------------------------------------------------ purchase screen

    public void openPurchase(Player player, Shop shop, Listing listing, int optionIndex, int quantity) {
        if (shop == null || listing == null) {
            return;
        }
        PaymentOption option = listing.option(optionIndex);
        if (option == null) {
            plugin.msg().send(player, "no-listings");
            return;
        }
        int max = plugin.trade().maxQuantity(player, shop, listing, optionIndex);
        int qty = Math.max(1, Math.min(quantity, Math.max(1, max)));
        Menu menu = create(player, Screen.PURCHASE, 6, "purchase-title", "name", esc(shop.name()));
        menu.shopId = shop.id();
        menu.listingId = listing.id();
        menu.option = optionIndex;
        menu.quantity = qty;

        ItemStack give = option.item().clone();
        give.setAmount(Math.max(1, option.amount()));
        set(menu, 12, itemIcon(give, "you-give", List.of(
                t("you-give-option", "amount", String.valueOf(option.amount() * qty),
                        "item", esc(Items.name(option.item()))),
                t("option-count", "count", String.valueOf(Items.count(player.getInventory(), option.item()))))));

        ItemStack get = listing.item().clone();
        get.setAmount(Math.min(get.getMaxStackSize(), Math.max(1, qty)));
        set(menu, 14, itemIcon(get, "you-get", List.of(
                t("you-get-item", "item", esc(Items.name(listing.item()))))));

        set(menu, 4, itemIcon(new ItemStack(Material.PAPER), "quantity",
                List.of(t("quantity-header",
                        "stock", shop.isAdmin() ? esc(plugin.msg().string("word-infinite", "infinite"))
                                : String.valueOf(plugin.shops().stock(shop, listing)),
                        "afford", String.valueOf(Items.count(player.getInventory(), option.item()) / option.amount()),
                        "room", String.valueOf(Items.room(player.getInventory(), listing.item()))))));

        if (listing.optionCount() > 1) {
            for (int i = 0; i < Math.min(4, listing.optionCount()); i++) {
                PaymentOption choice = listing.option(i);
                String key = i == optionIndex ? "option-select-active" : "option-select";
                set(menu, 28 + i, itemIcon(choice.item(), key,
                        List.of(t("option-lore")), "item", esc(Items.name(choice.item()))),
                        "option:" + i);
            }
        }

        set(menu, 37, icon(Material.LEVER, "qty-1"), "qty:1");
        set(menu, 38, icon(Material.LEVER, "qty-8"), "qty:8");
        set(menu, 39, icon(Material.LEVER, "qty-64"), "qty:64");
        set(menu, 40, icon(Material.GOLD_INGOT, "qty-max", "amount", String.valueOf(max)), "qty:max");
        set(menu, 41, icon(Material.ANVIL, "qty-custom"), "qty:custom");

        ItemStack confirm = icon(max > 0 ? Material.EMERALD_BLOCK : Material.BARRIER,
                "confirm", "amount", String.valueOf(qty),
                "item", esc(Items.name(listing.item())),
                "amountpay", String.valueOf(option.amount() * qty),
                "payitem", esc(Items.name(option.item())));
        set(menu, 46, confirm, max > 0 ? "confirm" : null);
        set(menu, 53, icon(Material.BARRIER, "cancel"), "shop:" + shop.id() + ":0");
        show(player, menu);
    }

    // ------------------------------------------------------------------ edit menus

    public void openEditor(Player player, Shop shop) {
        if (shop == null || !plugin.shops().mayManage(player, shop)) {
            plugin.msg().send(player, "not-your-shop");
            return;
        }
        Menu menu = create(player, Screen.EDIT, 6, "edit-title", "name", esc(shop.name()));
        menu.shopId = shop.id();
        UUID remembered = selectedListing.get(player.getUniqueId());
        if (remembered == null || shop.listing(remembered) == null) {
            remembered = shop.listings().isEmpty() ? null : shop.listings().get(0).id();
        }
        menu.listingId = remembered;
        menu.option = selectedOption.getOrDefault(player.getUniqueId(), 0);
        Listing selected = remembered == null ? null : shop.listing(remembered);

        List<Component> info = new ArrayList<>();
        info.add(t("owner-line", "owner", esc(plugin.shops().ownerName(shop))));
        if (shop.location() != null) {
            info.add(t("location-line", "x", String.valueOf(shop.location().getBlockX()),
                    "y", String.valueOf(shop.location().getBlockY()),
                    "z", String.valueOf(shop.location().getBlockZ()),
                    "world", esc(shop.location().getWorld().getName())));
        }
        info.add(t("toggle-open-lore", "state", shop.open() ? "open" : "closed"));
        set(menu, 4, itemIcon(new ItemStack(shop.isAdmin() ? Material.GOLD_BLOCK : Material.CHEST),
                "shop-entry-name", info, "name", esc(shop.name())));

        for (int i = 0; i < Math.min(7, shop.listings().size()); i++) {
            Listing listing = shop.listings().get(i);
            set(menu, 19 + i, itemIcon(listing.item(), "listing-name",
                    List.of(t("select-listing", "item", esc(Items.name(listing.item())))),
                    "item", esc(Items.name(listing.item()))),
                    "editlisting:" + listing.id());
        }
        for (int i = 0; selected != null && i < Math.min(4, selected.optionCount()); i++) {
            PaymentOption option = selected.option(i);
            set(menu, 28 + i, itemIcon(option.item(), "option-select",
                    List.of(t("option-lore")), "item", esc(Items.name(option.item()))),
                    "editoption:" + i);
        }
        if (selected != null && selected.optionCount() > 0) {
            PaymentOption option = selected.option(Math.min(menu.option, selected.optionCount() - 1));
            int suggested = plugin.settings().suggestedAmount(selected.item(), option.item());
            List<Component> priceLore = new ArrayList<>();
            priceLore.add(t("select-listing", "item", esc(Items.name(selected.item()))));
            if (suggested > 0) {
                priceLore.add(t("suggested-line", "amount", String.valueOf(suggested),
                        "item", esc(Items.name(option.item()))));
                if (plugin.settings().unfair(option.amount(), selected.item(), option.item())) {
                    priceLore.add(t("warning-line", "ratio",
                            String.format(Locale.ROOT, "%.1f", (double) option.amount() / suggested)));
                }
            }
            set(menu, 31, itemIcon(option.item(), "set-amount", priceLore));
        }

        set(menu, 10, icon(Material.ITEM_FRAME, "add-listing"), "edit:addlisting");
        set(menu, 12, icon(Material.GOLD_INGOT, "add-payment"), "edit:addpayment");
        set(menu, 14, icon(Material.ANVIL, "set-amount"), "edit:setamount");
        set(menu, 16, icon(Material.BARRIER, "remove-listing", "item",
                esc(selected == null ? "-" : Items.name(selected.item()))), "edit:removelisting");
        set(menu, 39, icon(Material.NAME_TAG, "rename"), "edit:rename");
        set(menu, 41, icon(Material.LEVER, "toggle-open", "state", shop.open() ? "open" : "closed"),
                "edit:toggle");
        set(menu, 43, icon(Material.VILLAGER_SPAWN_EGG, "link-entity"), "edit:link");
        if (shop.isAdmin()) {
            set(menu, 44, icon(Material.EMERALD, "allow-override"), "edit:allow");
            set(menu, 45, icon(Material.REDSTONE, "deny-override"), "edit:deny");
        }
        set(menu, 47, icon(Material.BARRIER, "close"), "shop:" + shop.id() + ":0");
        show(player, menu);
    }

    // --------------------------------------------------------------- collect & log

    public void openCollect(Player player, boolean admin) {
        Menu menu = create(player, Screen.COLLECT, 6, admin ? "admin-collect-title" : "collect-title");
        menu.adminCollect = admin;
        Shop target = admin ? adminShop() : null;
        List<ItemStack> items = admin && target != null ? target.collect() : new ArrayList<>();
        if (!admin) {
            for (Shop shop : plugin.shops().ownedBy(player.getUniqueId())) {
                items.addAll(shop.collect());
            }
        }
        for (int i = 0; i < Math.min(SLOTS.length, items.size()); i++) {
            ItemStack item = items.get(i).clone();
            ItemMeta meta = item.getItemMeta();
            meta.lore(List.of(t("collect-item")));
            item.setItemMeta(meta);
            set(menu, SLOTS[i], item, null);
        }
        if (items.isEmpty()) {
            set(menu, 22, icon(Material.BARRIER, "collect-empty"), null);
        }
        set(menu, 49, icon(Material.HOPPER, "take-all"), admin ? "takeall:admin" : "takeall:player");
        set(menu, 53, icon(Material.BARRIER, "back"), "main");
        show(player, menu);
    }

    public void openLog(Player player) {
        Menu menu = create(player, Screen.LOG, 6, "log-title");
        List<String> lines = plugin.logs().recent(plugin.settings().logLines());
        for (int i = 0; i < Math.min(lines.size(), SLOTS.length); i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(t("trade-log-line-title"));
            meta.lore(List.of(t("trade-log-line", "line", esc(lines.get(i)))));
            item.setItemMeta(meta);
            set(menu, SLOTS[i], item, null);
        }
        if (lines.isEmpty()) {
            set(menu, 22, icon(Material.BARRIER, "log-empty"), null);
        }
        set(menu, 53, icon(Material.BARRIER, "back"), "main");
        show(player, menu);
    }

    private Shop adminShop() {
        for (Shop shop : plugin.shops().all()) {
            if (shop.isAdmin()) {
                return shop;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- click routing

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0) {
            return;
        }
        String action = menu.actions.get(event.getRawSlot());
        if (action != null) {
            route(player, menu, action);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    private void route(Player player, Menu menu, String action) {
        String[] p = action.split(":");
        switch (p[0]) {
            case "close" -> player.closeInventory();
            case "main" -> openMain(player);
            case "browse" -> openList(player, p[1], parsePage(p[2]));
            case "shop" -> {
                Shop shop = plugin.shops().get(parse(p[1]));
                openShop(player, shop, parsePage(p[2]));
            }
            case "listing" -> {
                Shop shop = plugin.shops().get(menu.shopId);
                Listing listing = shop == null ? null : shop.listing(parse(p[1]));
                if (shop != null && shop.ownedBy(player.getUniqueId())) {
                    plugin.msg().send(player, "self-trade");
                    return;
                }
                openPurchase(player, shop, listing, 0, 1);
            }
            case "edit" -> {
                if (p.length > 1 && "open".equals(p[1])) {
                    Shop shop = plugin.shops().get(parse(p[2]));
                    openEditor(player, shop);
                } else {
                    editAction(player, menu, action.substring(5));
                }
            }
            case "option" -> {
                Shop shop = plugin.shops().get(menu.shopId);
                Listing listing = shop == null ? null : shop.listing(menu.listingId);
                if (shop != null && listing != null) {
                    openPurchase(player, shop, listing, Integer.parseInt(p[1]), menu.quantity);
                }
            }
            case "qty" -> quantity(player, menu, p[1]);
            case "confirm" -> {
                Shop shop = plugin.shops().get(menu.shopId);
                Listing listing = shop == null ? null : shop.listing(menu.listingId);
                if (shop == null || listing == null) {
                    plugin.msg().send(player, "trade-failed");
                    return;
                }
                plugin.trade().execute(player, shop, listing, menu.option, menu.quantity);
                player.closeInventory();
            }
            case "editlisting" -> {
                menu.listingId = parse(p[1]);
                menu.option = 0;
                selectedListing.put(player.getUniqueId(), menu.listingId);
                selectedOption.put(player.getUniqueId(), 0);
                Shop shop = plugin.shops().get(menu.shopId);
                if (shop != null) {
                    openEditor(player, shop);
                }
            }
            case "editoption" -> {
                menu.option = Integer.parseInt(p[1]);
                selectedOption.put(player.getUniqueId(), menu.option);
                Shop shop = plugin.shops().get(menu.shopId);
                if (shop != null) {
                    openEditor(player, shop);
                }
            }
            case "collect" -> openCollect(player, false);
            case "takeall" -> takeAll(player, p[1]);
            case "log" -> openLog(player);
            default -> {
            }
        }
    }

    private int parsePage(String raw) {
        return Math.max(0, Integer.parseInt(raw));
    }

    private void quantity(Player player, Menu menu, String raw) {
        Shop shop = plugin.shops().get(menu.shopId);
        Listing listing = shop == null ? null : shop.listing(menu.listingId);
        if (shop == null || listing == null) {
            return;
        }
        if (raw.equals("custom")) {
            int max = Math.max(1, plugin.trade().maxQuantity(player, shop, listing, menu.option));
            plugin.msg().send(player, "custom-prompt", "max", String.valueOf(max));
            plugin.prompts().request(player, ChatPromptService.Kind.CUSTOM_QUANTITY, text -> {
                int value;
                try {
                    value = Integer.parseInt(text);
                } catch (NumberFormatException ex) {
                    plugin.msg().send(player, "custom-invalid", "max", String.valueOf(max));
                    return;
                }
                if (value < 1 || value > max) {
                    plugin.msg().send(player, "custom-invalid", "max", String.valueOf(max));
                    return;
                }
                openPurchase(player, shop, listing, menu.option, value);
            });
            return;
        }
        int amount = raw.equals("max")
                ? Math.max(1, plugin.trade().maxQuantity(player, shop, listing, menu.option))
                : Integer.parseInt(raw);
        openPurchase(player, shop, listing, menu.option, amount);
    }

    private void takeAll(Player player, String kind) {
        int taken = 0;
        if (kind.equals("admin")) {
            Shop admin = adminShop();
            if (admin != null) {
                taken = plugin.shops().collectTakeAll(admin, player);
            }
        } else {
            for (Shop shop : plugin.shops().ownedBy(player.getUniqueId())) {
                taken += plugin.shops().collectTakeAll(shop, player);
            }
        }
        if (taken == 0) {
            plugin.msg().send(player, "collect-empty");
        } else {
            plugin.msg().send(player, "collected", "count", String.valueOf(taken));
        }
        openCollect(player, kind.equals("admin"));
    }

    // ------------------------------------------------------------- editor actions

    private void editAction(Player player, Menu menu, String action) {
        Shop shop = plugin.shops().get(menu.shopId);
        if (shop == null) {
            return;
        }
        Listing selected = shop.listing(menu.listingId);
        ItemStack held = player.getInventory().getItemInMainHand();
        switch (action) {
            case "addlisting" -> {
                if (held == null || held.getType().isAir()) {
                    plugin.msg().send(player, "create-no-item");
                    return;
                }
                Listing listing = new Listing(UUID.randomUUID(), held);
                shop.listings().add(listing);
                menu.listingId = listing.id();
                menu.option = 0;
                selectedListing.put(player.getUniqueId(), listing.id());
                selectedOption.put(player.getUniqueId(), 0);
                plugin.shops().save(shop);
                plugin.msg().send(player, "listing-added", "item", esc(Items.name(held)));
                openEditor(player, shop);
            }
            case "addpayment" -> {
                if (selected == null) {
                    plugin.msg().send(player, "no-selection");
                    return;
                }
                if (held == null || held.getType().isAir()) {
                    plugin.msg().send(player, "create-no-item");
                    return;
                }
                if (!plugin.settings().paymentAllowed(held.getType(), shop)) {
                    plugin.msg().send(player, shop.isAdmin() ? "payment-override-blocked" : "payment-blacklisted");
                    return;
                }
                if (selected.optionCount() >= 4) {
                    plugin.msg().send(player, "too-many-options");
                    return;
                }
                int suggested = plugin.settings().suggestedAmount(selected.item(), held);
                plugin.msg().send(player, "price-prompt", "item", esc(Items.name(held)));
                plugin.prompts().request(player, ChatPromptService.Kind.PAYMENT_AMOUNT, text -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(text);
                    } catch (NumberFormatException ex) {
                        plugin.msg().send(player, "price-invalid");
                        return;
                    }
                    if (amount < 1 || amount > 64) {
                        plugin.msg().send(player, "price-invalid");
                        return;
                    }
                    if (!selected.addOption(new PaymentOption(held, amount))) {
                        plugin.msg().send(player, "too-many-options");
                        return;
                    }
                    plugin.shops().save(shop);
                    plugin.msg().send(player, "payment-added", "item", Items.describe(held, amount));
                    if (suggested > 0 && plugin.settings().unfair(amount, selected.item(), held)) {
                        plugin.msg().send(player, "fairness-warning",
                                "ratio", String.format(Locale.ROOT, "%.1f", (double) amount / suggested),
                                "suggested", String.valueOf(suggested), "item", Items.name(held));
                    }
                    openEditor(player, shop);
                });
            }
            case "setamount" -> {
                if (selected == null || selected.optionCount() == 0) {
                    plugin.msg().send(player, "no-selection");
                    return;
                }
                int index = Math.min(menu.option, selected.optionCount() - 1);
                plugin.msg().send(player, "price-prompt", "item", esc(Items.name(selected.option(index).item())));
                plugin.prompts().request(player, ChatPromptService.Kind.PAYMENT_AMOUNT, text -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(text);
                    } catch (NumberFormatException ex) {
                        plugin.msg().send(player, "price-invalid");
                        return;
                    }
                    if (amount < 1 || amount > 64) {
                        plugin.msg().send(player, "price-invalid");
                        return;
                    }
                    selected.setOptionAmount(index, amount);
                    plugin.shops().save(shop);
                    plugin.msg().send(player, "payment-added", "item",
                            Items.describe(selected.option(index).item(), amount));
                    openEditor(player, shop);
                });
            }
            case "removelisting" -> {
                if (selected == null) {
                    return;
                }
                shop.removeListing(selected.id());
                menu.listingId = shop.listings().isEmpty() ? null : shop.listings().get(0).id();
                selectedListing.put(player.getUniqueId(), menu.listingId);
                plugin.shops().save(shop);
                plugin.msg().send(player, "listing-removed");
                openEditor(player, shop);
            }
            case "rename" -> {
                plugin.msg().send(player, "name-prompt");
                plugin.prompts().request(player, ChatPromptService.Kind.SHOP_NAME, text -> {
                    if (text.length() < 1 || text.length() > 32) {
                        plugin.msg().send(player, "name-invalid");
                        return;
                    }
                    shop.name(ChatColor.stripColor(text));
                    plugin.shops().save(shop);
                    plugin.msg().send(player, "renamed", "name", esc(shop.name()));
                    openEditor(player, shop);
                });
            }
            case "toggle" -> {
                if (!shop.open() && !shop.isAdmin() && plugin.shops().chest(shop) == null) {
                    plugin.msg().send(player, "chest-not-found");
                    return;
                }
                shop.open(!shop.open());
                plugin.shops().save(shop);
                plugin.msg().send(player, shop.open() ? "shop-reopened" : "shop-closed",
                        "name", esc(shop.name()));
                openEditor(player, shop);
            }
            case "link" -> {
                Entity target = player.getTargetEntity(6);
                if (target == null) {
                    plugin.msg().send(player, "villager-missing");
                    return;
                }
                shop.entityUuid(target.getUniqueId());
                plugin.shops().save(shop);
                plugin.msg().send(player, "villager-linked");
                openEditor(player, shop);
            }
            case "allow", "deny" -> {
                if (held == null || held.getType().isAir()) {
                    plugin.msg().send(player, "create-no-item");
                    return;
                }
                List<String> list = shop.paymentOverrideList();
                String name = held.getType().name();
                if (!list.contains(name)) {
                    list.add(name);
                }
                shop.paymentOverrideList(list);
                shop.paymentOverride(action.equalsIgnoreCase("allow") ? "ALLOW" : "DENY");
                plugin.shops().save(shop);
                plugin.msg().send(player, "payment-added", "item", Items.describe(held, 1));
                openEditor(player, shop);
            }
            default -> {
            }
        }
    }
}
