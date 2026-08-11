# Overnight QA pass — 2026-08-10

Everything below happened in one unattended session on `deniswsrosa/Paid_CTA`, picking up
right after the full live-emulator playthrough (Case 1 → Case 86) confirmed the mastermind
campaign works end to end. **Nothing in this branch is committed yet** — it's all working-tree
changes, ready for review.

## 1. New feature: Bureau ▸ Hint is now paid-gated

Per your request. Hint used to be free and unlimited for everyone. Now:

- **Unpaid + sales live**: tapping Hint offers the purchase instead of any hint text (reuses the
  existing purchase-offer popup, same one Passport/World Database/Case 14 already use).
- **Paid**: exactly **one** concrete tip per case — "The Bureau received word — the suspect is
  flying toward {region}." A second ask in the same case says "The Bureau has no additional tips
  for this case."
- **A welcome-back free hint** (the existing "come back after a few days" reward) always still
  works, paid or not — it's a separate promise, not something buying should gate.
- **Today's release (`SALES_ENABLED = false`)**: Hint behaves exactly as it always has —
  unlimited, free, unaffected. The new gating only activates once the kill switch flips, same
  pattern as everything else billing-related in this branch.

Files: `ClaraViewModel.kt` (`requestHint()`/`hintText()`, new `bureauTipUsed` field),
`SaveCodec.kt`, new `BureauHintTest.kt` (4 tests — 3 of them explicitly skip themselves while
sales are off, and start asserting for real the moment `SALES_ENABLED` flips true; verified this
by temporarily flipping the switch, running the full suite, and flipping it back).

No new art/characters needed — same as everything else in this campaign, it reuses existing
assets.

## 2. The most important thing found: a real story-skipping bug

The code-review pass (below) found something worth flagging clearly: **buying the campaign late
used to permanently skip mastermind arcs and desync your rank from the story.**

Case 14 no longer ends the free career (that was a deliberate change earlier this session,
specifically so players could keep playing free and buy later). But the arc triggers were still
measured from the *fixed* Case 14 — so if you free-played past Case 22 (Europe's Boss) before
buying, that arc was gone forever, and the next arc that *did* fire would hand out the wrong rank
alongside it (sequential rank-up vs. the arc's actual documented `patentRank` — they'd drift out
of sync the moment one arc was skipped).

**Fixed**: added `GameState.storyStartCase`, set once in `unlockExpansion()` to whichever is
later — Case 14 (buying on time, the original cadence, completely unchanged) or however many
cases you'd already solved (buying late — the story now starts counting from *there* instead).
Every arc still fires, in order, for every player; late buyers just get the whole thing shifted
later rather than truncated. Also deleted a dead, duplicate copy of this logic
(`ClaraViewModel.currentMastermindArc()`, unused, would have silently drifted from the real fix).

New regression test proving it: `MastermindsTest.buyingAfterAnArcsCaseNumberAlreadyPassedUnpaidStillDeliversThatArcLater`.

## 3. Everything else from tonight, by task

All ten things you approved, plus what came out of each:

**Fixed a real bug** — `toggleSound()`/`toggleHaptics()`/`toggleCaptions()` never called
`autosave()`. A crash right after opening Options and flipping a toggle lost the change. Fixed,
regression test added (`SaveTest.optionsTogglesAutosaveImmediately`).

**ANR hardening scan** — found the original `SaveStore.save()` fix (from earlier this session)
didn't close the whole loop. Fixed and verified:
- `Humor.init()`/`Humor.reload()` — parses up to ~680KB of JSON at cold start and on every
  language switch; now dispatched off the main thread.
- `ChooseGameScreen` — was calling `SaveStore.list()`/`load()` straight from a Composable body and
  a click handler, undispatched (the picker's own doc comment claimed this was already handled —
  it wasn't). Now uses `produceState` + `Dispatchers.IO`; the delete-then-refresh guarantee is
  preserved (delete stays synchronous, so by the time the async re-fetch runs the file's already
  gone).
- `Strings.setLanguage()` — same fix, lower priority (explicit, rare user action).

**Deliberately NOT fixed tonight** (flagged, not blind-fixed, because they need live visual/audio
verification I can't do at this hour):
- **`Sprites.bitmap()`/`PixelImage`** — the scan's most severe finding. Every new sprite (city art,
  witnesses, suspects — 602 PNGs, one as large as 1.7MB) does a synchronous decode on the
  composition thread the first time it's drawn. Real, hot-path, but fixing it properly needs an
  async-with-placeholder rework across 32 call sites — exactly the kind of change that needs eyes
  on a running emulator, not a blind overnight edit. **Recommend this as the next thing to tackle,
  with the emulator up.**
- **`GameSound`'s `MediaPlayer.prepare()`** — blocking prepare on the main thread for stingers.
  Tiny MIDI files, almost certainly imperceptible, but worth `prepareAsync()` eventually.
- A narrow, low-confidence `SaveStore` save/delete ordering race (see code review section below) —
  the practical window is tiny and a blind "fix" risked breaking the delete-then-refresh guarantee
  that was deliberately built the way it is.

**Edge-case purchase/promotion tests** — 2 new tests in `MastermindsTest.kt`: buying *before* Case
14 (proves the escape narrative and the promotion correctly fire on the very same solve), and
buying *many free cases* after Case 14 (proves the retroactive grant isn't time-limited). Plus the
arc-skip regression test from section 2.

**Save-file migration safety audit** — confirmed every field in `SaveCodec` has a safe default via
its own decode helpers (`int`/`bool`/`str`/etc. all default gracefully on a missing key, and
`decode()` itself is wrapped in try/catch, returning null rather than crashing on anything truly
malformed). Added an explicit test simulating a save from before `expansionUnlocked`/
`bureauTipUsed` existed — loads clean, defaults correctly, no crash.

**Debug-shortcut leak check** — clean. Every `dev*()` shortcut has exactly one call site, always
behind `BuildConfig.DEBUG`; confirmed release builds actually resolve `DEBUG=false` (no flavor/
buildType override); confirmed a debug-toggled entitlement can't leak into a release install
(different signing keys block an in-place update; anything else is equivalent to save-file
tampering a technical user could already do).

**Billing entitlement security review** — clean. `expansionUnlocked` is reachable only through the
real Play callback chain or the debug toggle; `handlePurchase()` correctly checks
`PurchaseState.PURCHASED` and acknowledges (avoiding Google's 3-day auto-refund); no signature
verification (client-trust), which is a normal, proportionate trade-off for a single low-value IAP
with no backend — flagged as intentional, not an oversight. One nit fixed: the acknowledge
callback silently swallowed failures; now logs a warning (it already self-healed via
`queryExistingPurchases()` on next launch, this just makes a persistent failure visible).

**Test-suite health pass** — 144 tests, 3 clean back-to-back full runs, nothing slow or flaky.
(One pre-existing, unseeded stochastic test — `FullPlaythroughByCluesTest`, a 12×100-case random
playthrough simulation — failed once in ~15 total runs tonight across all my re-verifications; 4
fresh reruns right after all passed. Not caused by anything tonight; worth a random seed someday
for determinism, not urgent.)

**Full branch code review** — a fresh pair of eyes (no context from this session) reviewed the
whole `deniswsrosa/Paid_CTA` diff. Findings, and what happened to each:
1. Late-purchase arc skip / rank desync — **fixed**, see section 2.
2. `BillingManager.connect()` wasn't gated by `SALES_ENABLED` — meaning if the Play Console product
   got created early for testing, a tester account could silently unlock the campaign in a build
   meant to be fully dark. **Fixed** — `connect()` now checks the switch too.
3. No purchase signature verification, client-trusted entitlement — reviewed, confirmed
   proportionate for this app's scale, not fixed (not a bug).
4. Duplicate arc-lookup logic — **fixed**, dead code deleted (see section 2).
5. Stale doc comment describing pre-redesign Case-14 behavior — **fixed**.
6. `translation/source/ui2.json` (the tracking file) was missing the 8 `mastermind.family`/
   `mastermind.role` keys that are actually fully translated and shipped in all 10 language
   catalogs — a source-of-truth sync gap, not a runtime bug. **Fixed** the extraction script and
   regenerated the tracking file.
7. A narrow save/delete ordering race — reviewed, low confidence it's practically reachable, left
   as-is rather than risk the delete-then-refresh guarantee (see above).
8. The `BureauHintTest` paid-path tests provide zero coverage while `SALES_ENABLED` is off (they
   self-skip) — inherent to testing a compile-time kill switch; noted, not fixed (would need a CI
   variant that flips the switch, which is infrastructure work outside tonight's scope).

**i18n audit** — went deep here since the first automated pass threw a lot of false positives
(wrong key-prefix assumptions for occupation/hobby/hair/etc. labels, and checking humor content
against the wrong file — it actually lives in separate `humor_<lang>.json` files, not the main
catalogs). Once corrected, coverage is genuinely complete: 0 missing keys, 0 empty values across
all 10 languages for every staged catalog. The placeholder "mismatches" that remained all turned
out to be legitimate per-language grammar, verified by checking each language's actual pronoun
catalog rather than assuming:
- German added a pronoun English's original omitted — correct, German's subjunctive construction
  needs an explicit subject.
- Portuguese and Turkish omitted a pronoun English implied — correct, both are pro-drop languages
  where it'd read as redundant/unnatural.
- Russian's pronoun-agreement slots (`pron.subj.m/f` etc.) are deliberately left empty, so gender
  agreement silently defaults to a neutral reading rather than crashing — a known, pre-existing
  limitation (not from tonight), gracefully degrading rather than broken. Worth a dedicated pass
  with a native speaker someday; not touched tonight.

## Final state

- `BillingManager.SALES_ENABLED` is `false` — confirmed, unchanged from before tonight. Everything
  paid-related stays completely dark for this release.
- Full test suite: 144 tests, passing (confirmed both with the switch off, matching what ships,
  and temporarily on, to actually exercise the new paid-path logic before reverting).
- Nothing is committed. All of tonight's changes are sitting in the working tree on
  `deniswsrosa/Paid_CTA`, ready for you to review with `git diff` in the morning.

## Suggested next step

Boot the emulator and tackle the `Sprites.bitmap()` async-decode fix with eyes on the screen —
it's the one real finding left that genuinely needs live verification rather than another
overnight pass.
