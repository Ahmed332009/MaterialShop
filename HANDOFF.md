# Handoff — MaterialShop marketplace rebuild

Branch: `arena/4018ac5c-materialshop`. The old `v0.1.0` GitHub Release is the earlier chest-centred build; this branch contains the marketplace rebuild and keeps jar version `0.1.0`.

## Done

- Stage 1: rebuilt skeleton under `com.banglaverse.materialshop`, plugin.yml aliases `/ms` and `/materialshop`, Actions jar verification updated.
- Stage 2: model (`Listing`, `PaymentOption`, `CollectBox`, `Shop`), `Items` matching, commented `config.yml`, `messages.yml` (`&` colors), atomic `DataStore`.
- Stage 3-5 (separate commits, pushed together): market GUIs, listing setup/management, purchase flow, collect box, server shops, villager NPC tags, optional chest-shop module, protection and simulated-break claim hook, trade engine, logger, prompts, commands.
- Stage 6: README with install, commands, permissions, config, decisions and the full manual test checklist; CHANGELOG; this handoff.

## Build status

GitHub Actions build is green on the final branch commit; jar artifact is `MaterialShop` on that run.

## Not done / untested

- No live Paper 26.3 server test. All in-game behaviour is unverified.
- No screenshots directory yet.
- Marketplace rebuild published as `v0.1.1` with `MaterialShop-0.1.0.jar` attached: https://github.com/Ahmed332009/MaterialShop/releases/tag/v0.1.1
- No automated unit tests; README has the manual checklist instead.
- Category tabs are capped at 18 configured categories plus All; larger config lists are ignored after that.

## Known bugs / limitations

- Listing creation time is not persisted separately, so newest-first sorting may reorder after a restart.
- Setup-menu drag is cancelled even in free slots; taps work for Java and Geyser.
- Collect-box overflow is dropped at world spawn with a console warning when the owner is offline; it is never silently deleted.
- A chest shop's listing stock comes from the live container; if the container is removed by an external plugin, trades on that shop fail cleanly.
- Server-shop payments are consumed, as specified; there is no admin earnings box in this build.

## Exact next steps

1. Download the jar from the green Actions run and test on Paper 26.3 with Geyser.
2. Follow the README checklist in order; fix bugs in the relevant file below, commit to this branch and keep Actions green.
3. Add screenshots to `docs/screenshots/` and link them in README.
4. After an in-game fix, push a new `v*` tag (for example `v0.1.2`) to publish a GitHub Release with the rebuilt jar. Do not overwrite `v0.1.0` or `v0.1.1`.

## What to do with each file

- `MaterialShop.java` — boot, services, listeners, commands and expiry task.
- `Settings.java`, `Messages.java` — config and `&`-color message loading.
- `Items.java` — `isSimilar` matching, counting, room and safe stack helpers.
- `model/` — listing, escrow, payment options, collect box and shop records.
- `storage/DataStore.java` — atomic YAML persistence; change this if a database is ever wanted.
- `trade/TradeEngine.java` — main-thread atomic trade order and rollback; `TradeLogger.java` logs; `Rewards.java` returns items.
- `menu/` — one class per screen; `MenuBase.java` owns click safety.
- `listener/` — menu click routing, chat prompts, villager NPC opening.
- `chest/` — optional chest shop manager and protection listener; `hook/ClaimHook.java` is the small simulated-break hook to replace with a real claims API later.
- `command/` — `/shop` and `/shopadmin` with tab completion.
- `src/main/resources/config.yml`, `messages.yml` — shipped defaults; add message keys for any new GUI text.
- `.github/workflows/build.yml` — build, verify, upload artifact and publish `v*` tags.
- `README.md`, `CHANGELOG.md`, `LICENSE`, `.gitignore`, `HANDOFF.md` — documentation and repo hygiene.

## Continuation prompt for another account

> Continue MaterialShop on branch `arena/4018ac5c-materialshop` in `Ahmed332009/MaterialShop`. Read HANDOFF.md and README.md first. The marketplace rebuild source is complete and the GitHub Actions build is green. Download the jar from the green run, test it on Paper 26.3 with Geyser using the README checklist, fix any bugs in `com.banglaverse.materialshop` sources, commit only to this branch and keep the build green. Then add screenshots and push a new `v*` tag to publish the next release. Do not overwrite `v0.1.0` or `v0.1.1`, do not switch branches, and do not add Vault, an economy dependency, NMS, reflection or shaded libraries.
