# MaterialShop

A Paper **barter marketplace**: players trade materials for materials. No money, no Vault, no economy plugin.
Companion to [MaterialClaim](https://github.com/Ahmed332009/MaterialClaim). Java players and Bedrock players
through GeyserMC use the same chest-inventory menus — no Bedrock forms, anvil GUIs, signs or dialogs.

Version **0.1.0**, GPL-3.0. The primary feature is the central `/shop` market: no chest or block is needed.
Chest shops are an optional extra that can be switched off in config.yml.

## Values copied from MaterialClaim (not guessed)

| Item | Value |
|---|---|
| Java | 21 (`JavaLanguageVersion.of(21)`) |
| Paper API | `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` |
| Repositories | `mavenCentral()` and `https://repo.papermc.io/repository/maven-public/` |
| Build tool | Gradle wrapper 8.10.2 (`gradlew`, Kotlin DSL) |
| Package style | one lowercase Java package per project; here `com.banglaverse.materialshop` |
| Config handling | `saveDefaultConfig()` + commented `config.yml`, reloaded without restart |
| `plugin.yml` | `api-version: '1.21'`, `${version}` expansion in resources |
| GitHub Actions | `actions/checkout@v5`, `actions/setup-java@v5`, jar verification, `actions/upload-artifact@v6` |
| License | GPL-3.0 `LICENSE` copied from MaterialClaim |

Target server: Paper 26.3 (Banglaverse). Paper API and Adventure Components only: **no NMS, no reflection, no shaded libraries**.

## Install

1. Download `MaterialShop-0.1.0.jar` from the `MaterialShop` artifact of a green
   [Build workflow run](../../actions), or from a release.
2. Put it in `plugins/` and restart the server.
3. Edit `plugins/MaterialShop/config.yml` and `messages.yml`, then `/shopadmin reload`.

Data is stored atomically in `plugins/MaterialShop/data.yml` (write tmp, then move). Trades append to
`plugins/MaterialShop/logs/transactions.log`, rotated by size.

## How the market works

- `/shop` opens the main menu: **Market**, **Server shop**, **My listings**, **Collect box**,
  **Create chest shop** and **My chest shops** (the last two only when chest shops are enabled).
- **Market** shows every active player listing with category tabs, pagination and newest/name sorting.
  Categories come from the `categories` section of config.yml; named/custom-model items go to Custom items.
- **My listings → New listing** opens the setup menu. Put the sale item in the marked slot (its stack size
  is one purchase unit), put up to four payment stacks in the payment slots (each is one payment option),
  choose stock units and confirm. Stock is taken into plugin escrow, so listings work while the seller is offline.
- Clicking a listing opens the purchase screen: **You give / You get**, payment selector, 1/8/64/Max,
  Custom quantity (chat prompt with `cancel` and timeout) and Confirm.
- Payments go to the seller's collect box. Sellers cannot buy their own listings.
- A seller can add stock, change each payment price, or cancel a listing. Cancelling (and optional expiry)
  returns escrow to the collect box.

Server shops are created with `/shopadmin create [name]`; they have infinite stock and consume payments.
A villager tagged with `/shopadmin npc <shop id>` opens its shop on a simple tap.

Chest shops are created with `/shop chest create [name]` while looking at a chest or barrel. Stock stays in
the container; other players tapping the container open the shop GUI, while the owner opens it normally.
Containers are protected from break, hoppers, explosions, pistons and adjacent double chests. Creation fires
a simulated `BlockBreakEvent` through the small `ClaimHook` class so land-claim plugins can veto it.

## Commands

| Command | Description |
|---|---|
| `/shop`, `/ms`, `/materialshop` | Main menu |
| `/shop list` | Your listings |
| `/shop collect` | Collect box |
| `/shop help` | In-game help |
| `/shop chest create [name]` | Chest shop (only if enabled) |
| `/shopadmin create [name]` | Create a server shop and add its first listing |
| `/shopadmin remove <id>` | Remove a server shop |
| `/shopadmin edit <id>` | Edit server-shop listings |
| `/shopadmin npc <id>` | Tag the villager you are looking at |
| `/shopadmin list` | Server shop ids |
| `/shopadmin reload` | Reload config and messages |
| `/shopadmin log` | Recent trades |

All commands have tab completion for subcommands and shop ids.

## Permissions

| Permission | Default |
|---|---|
| `materialshop.use` | true |
| `materialshop.create` | true |
| `materialshop.collect` | true |
| `materialshop.limit.<n>` | false (chest-shop limit) |
| `materialshop.admin` | op |
| `materialshop.admin.bypass` | op |

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `shop-limit` | 5 | Chest shops per player |
| `max-listings` | 20 | Market listings per player |
| `listing-expiry-days` | 0 | Optional expiry (0 = off) |
| `max-quantity` | 2304 | Hard cap per purchase |
| `collect-box-capacity` | 45 | Stacks per collect box |
| `prompt-timeout-seconds` | 30 | Chat prompt timeout |
| `payment-blacklist`, `payment-whitelist-enabled`, `payment-whitelist` | see file | Payment rules; server shops ignore them |
| `material-values`, `fairness-ratio` | see file | Warning only, never a block |
| `categories` | 10 tabs | Material-to-category mapping |
| `chest-shops.enabled` | true | Hides all chest-shop UI and commands when false |
| `logging.enabled`, `logging.rotate-megabytes` | true, 5 | Transaction log |
| `language-file` | messages.yml | Every GUI string lives here (`&` color codes, no emojis) |

## Decisions made where the brief was silent

- Escrow lives in `data.yml` as full serialized ItemStacks, not in a chest; one escrow stack is one purchase unit.
- Payment samples placed in the setup menu are copied as templates and returned to the player on confirm.
- Server-shop payments are consumed (no collect box) because the brief explicitly allows this for server shops; player and chest-shop trades have no fee or sink.
- The old WorldGuard/reflection hook from the earlier build was removed: this version uses only the simulated-break hook, which lets MaterialClaim and other claim plugins veto chest shop creation.
- Drag is cancelled in all menus, including setup slots; simple taps cover both Java and Geyser.
- Setup prices, stock units, custom quantities and shop names use chat prompts, never anvil GUIs or signs.
- The jar version stays `0.1.0` as requested. The previously published `v0.1.0` release contains the earlier chest-centred build; this marketplace rebuild should get a new release tag.

## Screenshots

*To be added after a live server test.* Put Java and Bedrock captures in `docs/screenshots/` and link them here.

## Manual test checklist

- [ ] **Normal trade:** list an item with one payment, buy 1, confirm; exact item and payment amounts.
- [ ] **Two payment options:** list two payment slots; each option can be selected and charges the right item.
- [ ] **Custom quantity:** chat prompt accepts a number; `cancel` and timeout both abort safely.
- [ ] **Max:** stops at the smaller of stock, affordable payment and free inventory space.
- [ ] **Full inventory:** trade refused with `no-room`; payment and escrow untouched.
- [ ] **Offline seller:** seller logs out; listing still sells and payment reaches the collect box.
- [ ] **Buying from your own listing:** refused with `self-buy`.
- [ ] **Two buyers in a row:** both complete; no duplicated or missing escrow units.
- [ ] **Restart persistence:** listings, escrow and collect boxes survive a restart with full item data.
- [ ] **Named and enchanted item:** only `isSimilar` items are accepted as payment or bought.
- [ ] **Backpack item:** a ProbablyBackpacks backpack keeps its NBT through listing, sale and collect.
- [ ] **Cancel listing returns escrow:** remaining units move to the collect box.
- [ ] **Expiry:** with `listing-expiry-days: 1`, an expired listing returns escrow.
- [ ] **Bedrock tap-only:** every menu action, including setup slots and custom quantity, works with taps.
- [ ] **Chest protection:** hoppers in/out, explosions, pistons and an adjacent chest cannot take or move shop stock.
- [ ] **Simulated break hook:** with MaterialClaim installed, a claimed block rejects `/shop chest create`.

## Build

```sh
./gradlew build
# build/libs/MaterialShop-0.1.0.jar
```

GitHub Actions builds with Java 21, checks the jar contents and expanded `plugin.yml`, then uploads the
`MaterialShop` artifact. Pushing a `v*` tag publishes a GitHub Release with the jar attached.

## License

GPL-3.0. See [LICENSE](LICENSE).
