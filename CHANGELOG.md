# Changelog

## 0.1.0 — marketplace rebuild (current branch)

- Rebuilt under `com.banglaverse.materialshop` with the MaterialClaim build values: Java 21, Paper 1.21.11 API, Gradle 8.10.2, `api-version: '1.21'`, GPL-3.0.
- Central `/shop` market as the primary feature: category tabs, pagination, newest/name sorting, no chest required.
- GUI listing setup: sale slot (stack size = unit), up to four payment slots, stock units, confirm. Escrow lives in `data.yml`.
- Listing management: add stock, change each price, cancel with escrow returned; optional expiry.
- Purchase screen with payment selector, 1/8/64/Max/Custom quantities, chat prompts and You give / You get confirmation.
- Server shops with infinite stock, `/shopadmin` tools and persistent-data-tagged villager NPCs.
- Optional chest shops with container stock, protection and a simulated-block-break claim hook. No reflection or NMS.
- Atomic trades on the main thread with snapshot rollback, listing locks, collect-box capacity check and size-rotated transaction log.
- `messages.yml` with `&` color codes for every player-visible string; full commented `config.yml`.
- Chest-only menus; shift-click, hotkeys, double-click, drag and offhand swaps cancelled outside controlled setup slots.

## 0.1.0 (original release, tag `v0.1.0`)

- First chest-centred shop build with admin/player shops and collected payments. Kept for history; superseded by the marketplace rebuild on this branch.
