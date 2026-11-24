# MaterialShop

A Minecraft Paper **barter shop**: players trade materials for materials. No money, no Vault, no economy plugin.
Companion to [MaterialClaim](https://github.com/Ahmed332009/MaterialClaim) — same Paper API level, Java version,
Gradle setup, package style, YAML persistence and GitHub Actions workflow.

Version **0.1.0** builds on GitHub Actions; the jar is uploaded as the `MaterialShop` artifact on every push.

## Features

- **Admin shops** with unlimited stock, created by staff with `/shopadmin`. A villager or NPC can be linked as a one-tap opening point.
- **Player shops** backed by a physical chest. Hold the item to sell, look at a chest (or place one), then set payments in the menu.
- One listing = one **exact** ItemStack (name, lore, enchantments, custom model data and NBT all preserved) sold for **1–4 alternative payment options**.
- Any material may pay unless it is on the configurable blacklist (or off the optional whitelist). Admin shops can override the rule per shop.
- Optional point **value table** shows a suggested price and a fairness warning. It never blocks a trade. No global exchange rates.
- Chest-inventory GUIs only — no anvil, signs or Bedrock forms. Every action is a simple tap, so Geyser players work the same way.
- Paginated shop lists and item grids, payment selector, 1/8/64/Max/Custom quantity buttons, chat-typed custom quantities with `cancel` and timeout.
- Offline sellers are paid into a **collect box**; a full collect box refuses the sale instead of losing items. No tax, fee or sink.
- Atomic main-thread trades with listing locks, full rollback, hopper/break/explosion/piston chest protection and an append-only transaction log.
- Soft, reflective MaterialClaim and WorldGuard checks. Neither plugin is required.

## Requirements

- Paper 26.3 (Banglaverse), built against the Paper **1.21.11** API (same as MaterialClaim).
- Java **21** or newer.
- GeyserMC/Floodgate optional, for Bedrock clients.

## Install

1. Download `MaterialShop-0.1.0.jar` from the `MaterialShop` artifact of a green
   [Build workflow run](../../actions), or from a release when one is published.
2. Put it in `plugins/` and restart the server.
3. Edit `plugins/MaterialShop/config.yml` and `plugins/MaterialShop/messages.yml`.
4. Run `/shopadmin reload` to apply changes without a restart.

Data lives in `plugins/MaterialShop/shops.yml`; trades are appended to
`plugins/MaterialShop/logs/transactions.log` (rotated by day and by size).

## Commands

| Command | Description |
|---|---|
| `/shop` | Main chest menu: admin shops, player shops, my shops, collect box |
| `/shop create [name]` | Hold the item to sell, target a chest or place one; then set payments in the editor |
| `/shop list` | Paginated list of your shops |
| `/shop collect` | Collect everything waiting in your collect box |
| `/shop help` | In-game help |
| `/shopadmin create [name]` | Create an admin shop at your target (held item becomes its first listing) |
| `/shopadmin edit [id]` | Edit an admin shop by id, or the shop you are looking at |
| `/shopadmin remove <id>` | Remove an admin shop (its earnings are returned to you) |
| `/shopadmin collect` | Collect admin-shop earnings |
| `/shopadmin log` | Show recent trades in a chest menu |
| `/shopadmin reload` | Reload config and messages |

`/materialshop` and `/ms` are aliases of `/shopadmin`. All commands have tab completion for
subcommands and shop ids.

## Permissions

| Permission | Default | Meaning |
|---|---|---|
| `materialshop.use` | true | Open `/shop` and trade |
| `materialshop.create` | true | Create player shops |
| `materialshop.collect` | true | Open the collect box |
| `materialshop.limit.<n>` | false | Raise the shop limit to `n` (highest matching number wins) |
| `materialshop.admin` | op | `/shopadmin`, admin shop editing, manage any shop |
| `materialshop.admin.bypass` | op | Ignore shop limits, chest and territory protections |

## Configuration

`config.yml` defaults: `shop-limit: 5`, `collect-box-rows: 6`, `chat-timeout-seconds: 30`,
payment blacklist (air, bedrock, barrier, command blocks, debug stick, spawn eggs, structure blocks and other
unobtainables), optional payment whitelist, an optional `material-values` table,
`fairness.warn-ratio: 4.0`, log rotation at 5 MB, and toggles for the MaterialClaim / WorldGuard hooks.
Every key is commented and has example entries. `language-file: messages.yml` chooses the message file;
every GUI title, button name and lore line lives there. Reload with `/shopadmin reload`.

## Decisions made where the brief was silent

- Payments always go into the **collect box**, whether the seller is online or offline. This keeps one safe path and means no payment is ever lost to a full inventory; both owner and staff collect with one tap.
- Admin shop earnings go to a **staff-collectable admin box**, never to a void/sink. Removing an admin shop returns its earnings to the staff member.
- Storage is **YAML with Bukkit's ItemStack serialization**, matching MaterialClaim (which uses YAML, not SQLite).
- Package name is `dev.materialshop` (lowercase), plugin.yml uses the same `api-version: '1.21'` as MaterialClaim.
- A linked shop entity can be any living entity (villager, NPC) identified by UUID; linking is done from the editor while looking at it.
- One payment option is 1–64 items; Max quantity is capped at 2304 (36 stacks of 64) so a trade stays one atomic inventory operation. Custom quantities are validated against stock, payment and free space.
- Shop creation requires holding the sale item at the time the chest is targeted or placed.

## Screenshots

*Screenshots will be added after the first live server test.* Put Java and Bedrock captures of the main menu,
an admin shop, a purchase screen and a collect box in `docs/screenshots/` and link them here.

## Manual test checklist

These are the acceptance checks for 0.1.0. They need a Paper server; the GitHub build only verifies compilation.

- [ ] **Normal trade:** admin shop with one payment option; buy 1, confirm, item and payment amounts are exact.
- [ ] **Multiple payment options:** listing with 3 options; switching options changes what is taken; all 4 options editable.
- [ ] **Custom quantity:** chat prompt accepts a number, `cancel` aborts, and the timeout message appears after the configured delay.
- [ ] **Max quantity:** stops at the smaller of stock, affordable payment and free inventory space; no partial transfer.
- [ ] **Full inventory:** trade is refused with the configured message; buyer payment and shop stock are untouched.
- [ ] **Offline seller:** player shop sale credits the collect box; owner collects with `/shop collect` or the main-menu button.
- [ ] **Full collect box:** sale is refused with `collect-full`; nothing is taken from the buyer.
- [ ] **Simultaneous buyers:** two players click the same last listing; exactly one succeeds, the other gets a refusal.
- [ ] **Restart persistence:** named/enchanted/custom-model items and a backpack-style NBT item survive restart and still match exactly.
- [ ] **Named and enchanted items:** payment template with a display name and enchants only accepts the exact same item.
- [ ] **Backpack items:** a ProbablyBackpacks backpack (or other NBT container) transfers with its data intact.
- [ ] **Bedrock tap-only:** with Geyser, all menus, payment selection, quantity buttons, collect and confirm work by tapping; no right-click or sign is needed.
- [ ] **Protection:** another player cannot open, break or hopper-drain a player-shop chest; explosions and pistons cannot move it.
- [ ] **Self-trade and limits:** owner cannot buy from own shop; sixth shop is refused unless `materialshop.limit.6` is held.
- [ ] **Transaction log:** each trade appends one line to `logs/transactions.log`; rotation creates a dated file.

## Build

```sh
./gradlew build
# jar: build/libs/MaterialShop-0.1.0.jar
```

The GitHub Actions workflow builds with Java 21, verifies that `plugin.yml` has an expanded version and that
the jar contains compiled classes, then uploads the jar as the `MaterialShop` artifact.

## License

GPL-3.0. See [LICENSE](LICENSE).
