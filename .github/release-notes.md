MaterialShop 0.1.0 — first release. Barter shop for Paper: trade materials for materials, with no money, Vault or economy plugin.

- Admin shops with unlimited stock, linked villager/NPC opening points and per-shop payment overrides.
- Player shops backed by a chest, with a 5-shop default limit and a collect box for offline sales.
- Exact ItemStack listings (name, enchants, custom model data and NBT preserved) with 1–4 payment options.
- Chest-only paginated GUIs for Java and Geyser/Bedrock, chat-typed custom quantities, no anvil GUIs or signs.
- Atomic trades with rollback, listing locks, chest protection and an append-only transaction log.
- Configurable payment blacklist/whitelist, optional value table and fairness warning, messages.yml and soft MaterialClaim/WorldGuard hooks.

Install: put the jar in `plugins/`, restart, edit `plugins/MaterialShop/config.yml`, then `/shopadmin reload`.
Requires Paper 26.3 (built against the Paper 1.21.11 API) and Java 21+.

The in-game manual test checklist is in the repository README. The GitHub Actions build and jar verification passed for this release.
