# Changelog

## 0.1.0 — first release (published 2026-11-24, built and jar-attached by GitHub Actions)

- Paper 1.21.11 API, Java 21, Gradle 8.10.2 wrapper, GitHub Actions jar artifact, GPL-3.0.
- Admin shops (`/shopadmin`) with unlimited stock, optional linked villager/NPC opening point and per-shop payment overrides.
- Player shops created from a targeted or newly placed chest; max 5 by default, raised by `materialshop.limit.<n>`.
- Exact ItemStack listings with 1–4 payment options; names, enchantments, custom model data and NBT are preserved.
- Chest-only paginated GUIs: main menu, shop lists, item grid, purchase screen, payment selector and collect box.
- Quantity buttons 1/8/64/Max/Custom; custom quantity via chat with `cancel` and timeout (no anvil GUI or sign).
- Atomic trades with per-listing locks, inventory snapshots, rollback, collect-box capacity check and main-thread-only mutation.
- Player and admin collect boxes; closing a player shop returns chest stock and collect-box content.
- Chest protection against other players, hoppers, explosions and pistons; reflective MaterialClaim and WorldGuard checks.
- Append-only `logs/transactions.log` with daily and size rotation; in-game trade-log menu.
- Configurable payment blacklist/whitelist, optional value table with fairness warning, message file, log and soft-dependency settings.
- `/shop`, `/shopadmin` (aliases `/materialshop`, `/ms`) with tab completion, plus all documented permissions.
