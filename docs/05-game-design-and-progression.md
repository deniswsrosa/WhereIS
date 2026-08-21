# 05 — Game design & progression

_How WhereIS plays: the destination roster, the free/paid split, and the implemented recognition-wave difficulty ladder. Updated 2026-08-21._

> Companion docs: per-country clue content lives in [`welcome_cards_state.md`](../welcome_cards_state.md); art pipeline in [`03-city-art-pipeline.md`](03-city-art-pipeline.md).

## 1. The core loop  [Current]

A **case** puts a thief on the run through a chain of destinations. At each stop the player interviews witnesses at venues, reads clues that point to the **next** destination, and flies there. Get the chain right and you corner the thief before the **deadline**; a warrant on the right suspect makes the arrest. Difficulty today comes almost entirely from **route length**, which grows with rank.

Key code: `game/ClaraViewModel.kt` — `newCase()` (route build), `arrive()` (landing + clock), `flightHoursTo()` (travel cost); `data/GameData.kt` (`ranks`, `CAREER_CASES`); `game/SpacedRepetition.kt` (which cities recur).

## 2. The roster

**231 destinations** total. All destination sets and postcard assets are wired into the game:

| Set | Count | Where | Postcard art |
|-----|------:|-------|-------------|
| Free original cities | 30 | `GameData.cities` / `CityMeta.all` / `WorldMap.pos` | all have `city_*.png` |
| Paid expansion (existing) | 68 | `Expansion.kt` | all have `city_*.png` (drawable not yet wired) |
| Paid expansion (new countries) | 133 | `welcome_cards_state.md` → to become `Expansion2.kt` | 133 ready, **0 pending** |
| **Total** | **231** | | **231 ready / 0 placeholder** |

Together these cover nearly every country on Earth.

## 3. Free vs paid  [Current]

- **Free** = the 30 original cities and the whole ~14-case career. All famous places, so the free game is the gentle on-ramp. Visited places are recorded for the passport from day one.
- **Paid** = the other 201 destinations and the ten-wave World Campaign. Play Billing grants `GameState.expansionUnlocked`. Purchase opens Wave 1 immediately; each wave-final capture and promotion opens the next wave.

## 4. Recognition waves = tiers  [Current]

Paid content unlocks in **10 recognition waves**, famous regions first, obscurity climbing. **The wave is the difficulty tier** — there is no separate fame scale. Each promotion recognizes you across one region ("you are now recognized as an agent by N more nations") and makes that wave's countries available; they then recur across future cases via spaced repetition.

| Wave | Region | Countries | Fame band | Examples |
|-----:|--------|----------:|-----------|----------|
| W1 | Europe — marquee | 33 | marquee | Amsterdam, Berlin, Cologne, Copenhagen |
| W2 | The Americas — marquee | 24 | marquee | Chichen Itza, Grand Canyon, Greenland, Iguazu Falls |
| W3 | Asia & the Middle East — marquee | 29 | marquee | Bali, Bhutan, Cambodia, Cappadocia |
| W4 | Africa — marquee | 19 | marquee | Abu Simbel, Cape Town, Casablanca, Kenya |
| W5 | Oceania — marquee | 5 | marquee | Easter Island, Honolulu, Palau, Fiji |
| W6 | Europe — lesser-known | 16 | lesser-known | Albania, Andorra, Belarus, Bosnia and Herzegovina |
| W7 | The Americas — lesser-known | 16 | lesser-known | Maranhao, Antigua and Barbuda, Barbados, Belize |
| W8 | Asia — lesser-known | 16 | lesser-known | Gujarat, Novosibirsk, Turkmenistan, Armenia |
| W9 | Africa — lesser-known | 33 | lesser-known | Angola, Benin, Botswana, Burkina Faso |
| W10 | Islands & frontiers | 10 | lesser-known | Antarctica, Tuvalu, Kiribati, Marshall Islands |
| | **Total paid** | **201** | | |

## 5. Patents (ranks) & difficulty  [Current]

The **free career keeps its 5 ranks** (Rookie → Ace Detective; 14 cases). Promotions fire at cases solved 1·5·9·13 and are gated by a quiz (`resolvePromotion`). Paid play is a finite ten-wave campaign: Wave 1 is available on purchase, each finale awards the patent shown below and opens the following wave, and Wave 10 ends with Chief Director and Clara's capture.

Difficulty rises through levers that **take turns** (never all at once): more **hops**, more **brand-new countries per case**, and a tighter **clock**. `Cases` = wins to the next promotion (not how many of a wave's countries you must visit).

| Patent | Wave / pool | Cases | Hops | New/case | Slack | Deadline | Pressure |
|--------|-------------|------:|-----:|---------:|------:|---------:|----------|
| Rookie | Free · 30 cities | 3 | 5 | 1 | 75% (30h) | 70h | Tutorial |
| Sleuth | Free · 30 cities | 3 | 6 | 1 | 58% (28h) | 76h | Tutorial |
| Private Eye | Free · 30 cities | 3 | 7 | 1 | 46% (26h) | 82h | Tutorial |
| Investigator | Free · 30 cities | 3 | 8 | 1 | 39% (25h) | 89h | Comfortable |
| Ace Detective | Free · 30 cities | 2 | 9 | 1 | 33% (24h) | 96h | Comfortable |
| ✦ Special Agent | W1 Europe — marquee (+33) | 8 | 9 | 1 | 33% (24h) | 96h | Comfortable |
| ✦ Field Inspector | W2 The Americas — marquee (+24) | 8 | 9 | 1 | 32% (23h) | 95h | Comfortable |
| ✦ Senior Inspector | W3 Asia & the Middle East — marquee (+29) | 8 | 10 | 1 | 28% (22h) | 102h | Comfortable |
| ✦ Inspector | W4 Africa — marquee (+19) | 8 | 10 | 2 | 25% (20h) | 100h | Comfortable |
| ✦ Chief Inspector | W5 Oceania — marquee (+5) | 10 | 10 | 2 | 24% (19h) | 99h | Steady |
| ✦ Superintendent | W6 Europe — lesser-known (+16) | 10 | 11 | 2 | 20% (18h) | 106h | Steady |
| ✦ Commander | W7 The Americas — lesser-known (+16) | 10 | 11 | 3 | 19% (17h) | 105h | Tense |
| ✦ Deputy Director | W8 Asia — lesser-known (+16) | 12 | 11 | 3 | 18% (16h) | 104h | Tense |
| ✦ Director | W9 Africa — lesser-known (+33) | 12 | 12 | 3 | 16% (15h) | 111h | Tense |
| ✦ Chief Director | W10 Islands & frontiers (+10) | 12 | 12 | 3 | 16% (15h) | 111h | Tense |

Campaign wave lengths are **8, 8, 8, 8, 10, 10, 10, 12, 12, 12** cases. Only a wave's last case is region-restricted. Clara appears six times: Case 14 and the Wave 6–10 finales.

## 6. The clock  [Current]

The deadline is a **travel-time budget**, not a wall-clock timer:

- **[Current]** `DEADLINE_HOURS = 152` (Mon 9am → Sun 5pm). Each flight costs `flightHoursTo = (2 + distance·6)` clamped to **2–14h** (measured average **~5h**). Landing between 10pm and 8am **rolls the clock to 8am** (a lost night). Investigating venues is free; a wrong flight wastes a flight's worth of hours. Net cost of a hop ≈ **8 clock-hours**.
- The generated deadline uses a simulated efficient run plus rank slack, so it always covers the authored route. Paid players can optionally enable an additional **+8-hour Travel Buffer**.

## 7. Difficulty is fair — the check  [Proposed]

Each patent is validated with a simple model so the curve never becomes a coin-flip:

```
expected wrong guesses   E = 0.05·(familiar hops) + 0.30·(new/obscure hops)
wrong-guess budget       B = slack ÷ 8h        (a wrong flight ≈ 8h)
margin                   M = B − E             (must stay > 0)
```

Across all patents the margin glides **+3.25 → +0.53** (Tutorial → Tense), always positive. Two rules fall out of it: **new/case is capped at 3** (4 pushed the top into losing territory), and the **first case that introduces any new country gets a slack bump + an extra dossier hint**, so novelty is never punished by the tight clock. The constants (`0.05`, `0.30`, the `8h` wrong-flight cost) are the values to calibrate against playtests.

## 8. Reveal cadence — new vs. familiar  [Current]

A wave *unlocks* its countries but you don't meet them all at once. Per case, at most **`new/case`** destinations are first-sightings; the rest are countries you've already seen, chosen by the existing `SpacedRepetition.pickRoute` (never-seen 3.0, review-due 4.0, just-seen 0.4). So the world stays learnable and weak spots resurface.

## 9. Postcards  [Current fallback, Proposed resolution]

Every destination shows a briefing postcard. **[Current]** `ui/screens/GameScreens.kt` `CityPhoto()` draws `CityInfo.drawable` (a `city_*` sprite from `assets/sprites/cities/`) or falls back to the procedural **`VgaCityCard`** placeholder — so a missing image never blocks a case. **[Proposed]** add a `postcard` field to `CityInfo` and resolve `country_<slug>` → mapped `city_*` → `VgaCityCard`. Coverage today: **231/231** have real art; the **0** pending are new countries, tracked so art can backfill in wave order (famous first).

## 10. Passport  [Current]

`GameState.visitedPlaces` records every landing from day one (`data/CountryShapes.kt` maps place → country). Free stamps stay visible on the painted map; the remaining passport is frosted and locked until purchase. The World Database similarly previews one real fact for a locked place, with the remaining facts gated by entitlement and earned wave.

## 11. Implementation map  [Current]

1. `Expansion2.kt` supplies the final 133 destinations; `Progression.wave` is the wave gate.
2. `Masterminds.kt` owns the ten story finales and variable-length cadence.
3. `ClaraViewModel.newCase()` mixes the currently earned waves into ordinary routes and restricts only finale routes.
4. `GameState.expansionUnlocked` is the durable entitlement; the highest wave derives from rank so it cannot drift out of sync.
5. `GameMenuBar.kt` owns the shared purchase surface, Passport/Database previews, Case Planner and ceremony.

## 12. Tunable knobs

Wave boundaries & order · countries per wave · `new/case` cap · slack % ramp and floor · per-hop cost (`PERHOP`) · wrong-flight cost & error rates (§7) · cases per patent · free-career length. All are isolated constants; changing one re-derives the tables above.
