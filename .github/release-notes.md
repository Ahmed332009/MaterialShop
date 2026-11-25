MaterialShop 0.1.1 — rebuilt around the central GUI marketplace. The jar inside is `MaterialShop-0.1.0.jar` (project version kept at 0.1.0 as specified); the old v0.1.0 release remains the earlier chest-centred build.

- `/shop` market: category tabs, pagination, newest/name sorting, no chest needed.
- Fully GUI listing setup: sale slot, up to four payment slots, stock units and confirm; escrow stored in data.yml.
- Listing management: add stock, change prices, cancel with escrow returned, optional expiry.
- Purchase screen: payment options, 1/8/64/Max, chat-typed Custom quantity, You give / You get confirm.
- Server shops with infinite stock and villager NPCs tagged through persistent data.
- Optional chest shops with container stock, claim-veto hook and protection from hoppers, explosions, pistons and double chests.
- Atomic main-thread trades with snapshot rollback, listing locks, collect-box capacity check and size-rotated transaction log.
- Paper API and Adventure Components only: no NMS, no reflection, no shaded libraries, no Vault.

Requires Paper 26.3 (built against Paper 1.21.11 API) and Java 21+. GeyserMC is supported through the same chest menus. Read the repository README for installation and the full manual test checklist.
