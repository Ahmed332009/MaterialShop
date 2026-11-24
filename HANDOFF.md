# Handoff — MaterialShop 0.1.0

Branch: `arena/4018ac5c-materialshop`. Base: `721e4ac` of `main`. Work is committed and pushed to this branch.

## Done

- Stage 1: skeleton, `build.gradle.kts`, Gradle wrapper from MaterialClaim, `plugin.yml`, Actions workflow, GPL-3.0 `LICENSE`, `.gitignore`.
- Stage 2: data model (`Shop`, `Listing`, `PaymentOption`), exact `Items` matching, YAML `ShopStorage` with Bukkit ItemStack serialization, `SettingsService`, `Messages`.
- Stage 3: admin shops, all chest GUIs, editor, chat prompts, atomic `TradeService`, `TradeLogger`, commands and tab completion.
- Stage 4: player shop creation (target or place chest), collect box, close-shop returns.
- Stage 5: chest protection, hopper/explosion/piston guards, reflective MaterialClaim and WorldGuard hooks, territory check on creation.
- Stage 6: full `config.yml`, `messages.yml`, README install/commands/permissions/config/screenshots/test checklist, CHANGELOG, this handoff.

## Build status

Every stage was compiled by GitHub Actions. Last green run: check the latest `Build` workflow on this branch;
the jar is the `MaterialShop` artifact (`build/libs/MaterialShop-0.1.0.jar`).

## Not done / untested

- No live Paper server test: trades, GUIs, Geyser taps, permissions and soft dependencies are **unverified in game**.
- No automated unit tests; the README contains the manual acceptance checklist instead.
- No screenshots yet (README has a section reserved for them).
- GitHub Release `v0.1.0` is published with `MaterialShop-0.1.0.jar` attached: https://github.com/Ahmed332009/MaterialShop/releases/tag/v0.1.0
- Chest protection uses the chest's stored location; if a player moves a shop chest with a plugin that bypasses pistons, re-link by recreating the shop.
- Admin-shop per-shop payment overrides are set from the editor with held items, not from config.

## Known bugs / limitations

- Chat prompts and shop creation modes are per-player and in memory; a server restart cancels them (shop data itself persists).
- The transaction log reader shows the newest lines of the current file only, not rotated files.
- `language-file` copies the shipped `messages.yml` when a different name is configured; it does not translate anything.
- Max quantity is capped at 2304 to keep one atomic transfer; larger purchases must be split.

## Exact next steps

1. Upload the jar to a Paper 26.3 test server with Java 21 (and Geyser for Bedrock checks).
2. Work through the README checklist in order; watch `plugins/MaterialShop/logs/transactions.log` and server console.
3. Fix any in-game issue in the relevant file (map below), commit to this branch and let Actions rebuild.
4. When the checklist is green, take screenshots into `docs/screenshots/` and publish `v0.1.1` (push a `v*` tag; the workflow attaches the jar automatically).

## What to do with each file

- `src/main/java/dev/materialshop/MaterialShop.java` — plugin bootstrap, service wiring, reload command hook.
- `SettingsService.java`, `Messages.java` — config/message loading; add keys here when adding player-facing text.
- `Shop.java`, `Listing.java`, `PaymentOption.java` — in-memory model; add fields here and to `ShopStorage` together.
- `ShopStorage.java` — YAML load/save; change this if moving to SQLite (not done; MaterialClaim uses YAML).
- `Items.java` — exact matching, counting, room calculation and collect-box arithmetic; all duplication-sensitive logic lives here.
- `TradeService.java`, `TradeLogger.java` — atomic trades, rollback, locking and log rotation.
- `GuiManager.java`, `ChatPromptService.java` — chest menus, click routing, chat prompts.
- `ShopCommand.java`, `ShopAdminCommand.java`, `ShopEntityListener.java`, `ProtectionListener.java`, `TerritoryHook.java` — commands, entity opening, chest protection and soft dependencies.
- `src/main/resources/config.yml`, `messages.yml` — shipped defaults; every GUI string must have a key in `messages.yml`.
- `.github/workflows/build.yml` — build, jar verification and artifact upload.
- `README.md`, `CHANGELOG.md`, `HANDOFF.md`, `LICENSE`, `.gitignore` — documentation and repo hygiene.

## Continuation prompt for another account

> Continue MaterialShop on branch `arena/4018ac5c-materialshop` in the `Ahmed332009/MaterialShop` repo.
> Read HANDOFF.md and README.md first. The 0.1.0 source is complete and the GitHub Actions build is green.
> Test the jar from the latest Actions artifact on a Paper 26.3 server using the README checklist, fix any
> in-game bugs in `dev.materialshop` sources, commit to this branch only, and keep the Actions build green.
> `v0.1.0` is already released with the jar attached. Do not switch branches; do not add Vault or an economy dependency. After an in-game fix, push a new `v*` tag to publish the next release with its jar.
