#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Emits docs/05-game-design-and-progression.md with tables generated from the live data.
import re, json, io

def parse(p): return re.findall(r'CityInfo\("([^"]+)",\s*"([^"]+)"', open(p,encoding='utf-8').read())
BASE="/home/deniswsrosa/Documents/projects/WhereIS/android/app/src/main/java/com/acme/clara/data/"
orig=parse(BASE+"CityMeta.kt"); exp=parse(BASE+"Expansion.kt")
new=json.load(open('country_strategy.json'))
EXP_TIER={ "Amsterdam":1,"Berlin":1,"Dubai":1,"Cape Town":1,"Copenhagen":1,"Edinburgh":1,"Mount Everest":1,"Grand Canyon":1,"Hong Kong":1,"Jerusalem":1,"Las Vegas":1,"Los Angeles":1,"Prague":1,"San Francisco":1,"Santorini":1,"Stonehenge":1,"Venice":1,"Vienna":1,"Washington DC":1,"Chichen Itza":1,"Easter Island":1,"Iguazu Falls":1,"Vatican City":1,"Pisa":1,"Pompeii":1,"Mount Rushmore":1,"Neuschwanstein":1,"Bali":1,"Cambodia":1,"Ha Long Bay":1,"Marrakech":1,"Lisbon":1,"Krakow":1,"Dubrovnik":1,"Ireland":1,"Jordan":1,"Saint Petersburg":1,"Monaco":1,"Kuala Lumpur":1,"Honolulu":1,"Abu Simbel":2,"Cappadocia":2,"Casablanca":2,"Cologne":2,"Greenland":2,"Mongolia":2,"Myanmar":2,"Northern Ireland":2,"Scottish Highlands":2,"Valencia":2,"Mauritius":2,"Palau":2,"Samarkand":2,"Salar de Uyuni":2,"Svalbard":2,"Kenya":2,"Mont Saint-Michel":2,"Bhutan":2,"Taipei":2,"Toronto":2,"Seattle":2,"Philippines":2,"Gujarat":3,"Novosibirsk":3,"Turkmenistan":3,"Tuvalu":3,"Maranhao":3,"Antarctica":3 }
def cont(r):
    r=r.lower()
    if 'middle east' in r or r=='asia': return 'Asia'
    if 'america' in r: return 'Americas'
    if 'oceania' in r: return 'Oceania'
    if 'antarctic' in r: return 'Antarctica'
    if 'africa' in r: return 'Africa'
    if 'europe' in r: return 'Europe'
    return r.title()
roster=[]
for n,r in orig: roster.append({"name":n,"cont":cont(r),"tier":0,"paid":False,"art":True})
for n,r in exp:  roster.append({"name":n,"cont":cont(r),"tier":EXP_TIER[n],"paid":True,"art":True})
for c in new:    roster.append({"name":c['name'],"cont":c['continent'],"tier":c['tier'],"paid":True,"art":c['postcard']['has']})
def band(t): return "core" if t in (1,2) else "deep"
WAVES=[("Europe — marquee",{"Europe"},"core"),("The Americas — marquee",{"Americas"},"core"),
 ("Asia & the Middle East — marquee",{"Asia"},"core"),("Africa — marquee",{"Africa"},"core"),
 ("Oceania — marquee",{"Oceania"},"core"),("Europe — lesser-known",{"Europe"},"deep"),
 ("The Americas — lesser-known",{"Americas"},"deep"),("Asia — lesser-known",{"Asia"},"deep"),
 ("Africa — lesser-known",{"Africa"},"deep"),("Islands & frontiers",{"Oceania","Antarctica"},"deep")]
def wave_of(c):
    if not c['paid']: return -1
    for i,(lab,cs,bd) in enumerate(WAVES):
        if c['cont'] in cs and band(c['tier'])==bd: return i
    return -1
for c in roster: c['wave']=wave_of(c)
wave_ct=[sum(1 for c in roster if c['wave']==i) for i in range(len(WAVES))]
def examples(i,k=4): return ", ".join([c['name'] for c in roster if c['wave']==i][:k])
art=sum(1 for c in roster if c['art']); TOT=len(roster)
new_art=sum(1 for c in new if c['postcard']['has']); new_miss=len(new)-new_art

# difficulty model
PERHOP=8; WRONG=8; PK=0.05; PN=0.30; FLOOR=15
PAT=["Special Agent","Field Inspector","Senior Inspector","Inspector","Chief Inspector","Superintendent","Commander","Deputy Director","Director","Chief Director"]
CASES=[8,8,8,8,10,10,10,12,12,12]; HOPS=[9,9,10,10,10,11,11,11,12,12]; NEWC=[1,1,1,2,2,2,3,3,3,3]; SLACK=[24,23,22,20,19,18,17,16,15,15]
FREE=[["Rookie",3,5,1,30],["Sleuth",3,6,1,28],["Private Eye",3,7,1,26],["Investigator",3,8,1,25],["Ace Detective",2,9,1,24]]
def press(m): return "Tutorial" if m>=2.5 else "Comfortable" if m>=1.5 else "Steady" if m>=0.9 else "Tense" if m>=0.5 else "On the edge"
def prow(pat,wave,cases,hops,nc,slack):
    slack=max(FLOOR,slack); minr=hops*PERHOP; dl=minr+slack; buf=round(slack/minr*100)
    E=PK*(hops-nc)+PN*nc; m=slack/WRONG-E
    return (pat,wave,cases,hops,nc,buf,slack,dl,press(m),round(m,2))

L=[]
L.append("# 05 — Game design & progression")
L.append("")
L.append("_How WhereIS plays: the destination roster, the free/paid split, and the recognition-wave difficulty ladder. Generated 2026-08-03. Sections are tagged **[Current]** (in the shipped code) or **[Proposed]** (the expansion design, not yet implemented)._")
L.append("")
L.append("> Companion docs: per-country clue content lives in [`welcome_cards_state.md`](../welcome_cards_state.md); art pipeline in [`03-city-art-pipeline.md`](03-city-art-pipeline.md).")
L.append("")
L.append("## 1. The core loop  [Current]")
L.append("")
L.append("A **case** puts a thief on the run through a chain of destinations. At each stop the player interviews witnesses at venues, reads clues that point to the **next** destination, and flies there. Get the chain right and you corner the thief before the **deadline**; a warrant on the right suspect makes the arrest. Difficulty today comes almost entirely from **route length**, which grows with rank.")
L.append("")
L.append("Key code: `game/ClaraViewModel.kt` — `newCase()` (route build), `arrive()` (landing + clock), `flightHoursTo()` (travel cost); `data/GameData.kt` (`ranks`, `CAREER_CASES`); `game/SpacedRepetition.kt` (which cities recur).")
L.append("")
L.append("## 2. The roster")
L.append("")
L.append(f"**{TOT} destinations** total. **[Proposed]** the 133 new countries are not wired into the game yet, and {new_miss} still lack postcard art:")
L.append("")
L.append("| Set | Count | Where | Postcard art |")
L.append("|-----|------:|-------|-------------|")
L.append("| Free original cities | 30 | `GameData.cities` / `CityMeta.all` / `WorldMap.pos` | all have `city_*.png` |")
L.append("| Paid expansion (existing) | 68 | `Expansion.kt` | all have `city_*.png` (drawable not yet wired) |")
L.append(f"| Paid expansion (new countries) | 133 | `welcome_cards_state.md` → to become `Expansion2.kt` | {new_art} ready, **{new_miss} pending** |")
L.append(f"| **Total** | **{TOT}** | | **{art} ready / {TOT-art} placeholder** |")
L.append("")
L.append("Together these cover nearly every country on Earth.")
L.append("")
L.append("## 3. Free vs paid  [Current gate, Proposed model]")
L.append("")
L.append("- **Free** = the 30 original cities and the whole ~14-case career. All famous places, so the free game is the gentle on-ramp. Visited places are recorded for the passport from day one.")
L.append("- **Paid** = the other 201 destinations. **[Current]** they sit behind a single boolean `GameState.expansionUnlocked` (flipped only by `unlockExpansion()`; no store/IAP is wired yet). **[Proposed]** replace that all-or-nothing switch with a **rank-earned reveal**: buying the pack unlocks the *ability to earn* countries, which then arrive wave by wave as you rank up.")
L.append("")
L.append("## 4. Recognition waves = tiers  [Proposed]")
L.append("")
L.append("Paid content unlocks in **10 recognition waves**, famous regions first, obscurity climbing. **The wave is the difficulty tier** — there is no separate fame scale. Each promotion recognizes you across one region (\"you are now recognized as an agent by N more nations\") and makes that wave's countries available; they then recur across future cases via spaced repetition.")
L.append("")
L.append("| Wave | Region | Countries | Fame band | Examples |")
L.append("|-----:|--------|----------:|-----------|----------|")
for i,(lab,cs,bd) in enumerate(WAVES):
    L.append(f"| W{i+1} | {lab} | {wave_ct[i]} | {'marquee' if bd=='core' else 'lesser-known'} | {examples(i)} |")
L.append(f"| | **Total paid** | **{sum(wave_ct)}** | | |")
L.append("")
L.append("## 5. Patents (ranks) & difficulty  [Proposed]")
L.append("")
L.append("The **free career keeps its 5 ranks** (Rookie → Ace Detective; ~14 cases; reach Ace after ~12 wins). **[Current]** promotions fire at cases solved 1·5·9·13 and are gated by a quiz (`resolvePromotion`). Paid play then opens an **endless \"International\" track** — one new grade (patent) per wave, past Ace Detective.")
L.append("")
L.append("Difficulty rises through levers that **take turns** (never all at once): more **hops**, more **brand-new countries per case**, and a tighter **clock**. `Cases` = wins to the next promotion (not how many of a wave's countries you must visit).")
L.append("")
L.append("| Patent | Wave / pool | Cases | Hops | New/case | Slack | Deadline | Pressure |")
L.append("|--------|-------------|------:|-----:|---------:|------:|---------:|----------|")
for nm,cs,hp,nc,sl in FREE:
    p=prow(nm,-1,cs,hp,nc,sl)
    L.append(f"| {p[0]} | Free · 30 cities | {p[2]} | {p[3]} | {p[4]} | {p[5]}% ({p[6]}h) | {p[7]}h | {p[8]} |")
for i in range(len(WAVES)):
    p=prow(PAT[i],i,CASES[i],HOPS[i],NEWC[i],SLACK[i])
    L.append(f"| ✦ {p[0]} | W{i+1} {WAVES[i][0]} (+{wave_ct[i]}) | {p[2]} | {p[3]} | {p[4]} | {p[5]}% ({p[6]}h) | {p[7]}h | {p[8]} |")
L.append("")
L.append("## 6. The clock  [Current mechanic, Proposed slack]")
L.append("")
L.append("The deadline is a **travel-time budget**, not a wall-clock timer:")
L.append("")
L.append("- **[Current]** `DEADLINE_HOURS = 152` (Mon 9am → Sun 5pm). Each flight costs `flightHoursTo = (2 + distance·6)` clamped to **2–14h** (measured average **~5h**). Landing between 10pm and 8am **rolls the clock to 8am** (a lost night). Investigating venues is free; a wrong flight wastes a flight's worth of hours. Net cost of a hop ≈ **8 clock-hours**.")
L.append("- **[Proposed]** express the difficulty as **slack** — a percentage buffer over the flight-time a route *must* burn, floored at **15h** — instead of an absolute deadline. So `deadline = hops·8h + slack`, always covering the route; slack runs **~75% → ~16%** across the ladder. This is what makes late cases tense without ever being impossible.")
L.append("")
L.append("## 7. Difficulty is fair — the check  [Proposed]")
L.append("")
L.append("Each patent is validated with a simple model so the curve never becomes a coin-flip:")
L.append("")
L.append("```")
L.append("expected wrong guesses   E = 0.05·(familiar hops) + 0.30·(new/obscure hops)")
L.append("wrong-guess budget       B = slack ÷ 8h        (a wrong flight ≈ 8h)")
L.append("margin                   M = B − E             (must stay > 0)")
L.append("```")
L.append("")
L.append("Across all patents the margin glides **+3.25 → +0.53** (Tutorial → Tense), always positive. Two rules fall out of it: **new/case is capped at 3** (4 pushed the top into losing territory), and the **first case that introduces any new country gets a slack bump + an extra dossier hint**, so novelty is never punished by the tight clock. The constants (`0.05`, `0.30`, the `8h` wrong-flight cost) are the values to calibrate against playtests.")
L.append("")
L.append("## 8. Reveal cadence — new vs. familiar  [Proposed]")
L.append("")
L.append("A wave *unlocks* its countries but you don't meet them all at once. Per case, at most **`new/case`** destinations are first-sightings; the rest are countries you've already seen, chosen by the existing `SpacedRepetition.pickRoute` (never-seen 3.0, review-due 4.0, just-seen 0.4). So the world stays learnable and weak spots resurface.")
L.append("")
L.append("## 9. Postcards  [Current fallback, Proposed resolution]")
L.append("")
L.append(f"Every destination shows a briefing postcard. **[Current]** `ui/screens/GameScreens.kt` `CityPhoto()` draws `CityInfo.drawable` (a `city_*` sprite from `assets/sprites/cities/`) or falls back to the procedural **`VgaCityCard`** placeholder — so a missing image never blocks a case. **[Proposed]** add a `postcard` field to `CityInfo` and resolve `country_<slug>` → mapped `city_*` → `VgaCityCard`. Coverage today: **{art}/{TOT}** have real art; the **{TOT-art}** pending are new countries, tracked so art can backfill in wave order (famous first).")
L.append("")
L.append("## 10. Passport  [Current]")
L.append("")
L.append("`GameState.visitedPlaces` records every landing from day one (`data/CountryShapes.kt` maps place → country). The painted world map (`ui/GameMenuBar.kt` `PassportWindow`) is gated by the same `expansionUnlocked` flag. **[Proposed idea]** promote it from \"visited\" to \"mastered\" — a country fills in only after you've correctly routed through it a few times — to give the map real meaning.")
L.append("")
L.append("## 11. Implementation checklist  [Proposed]")
L.append("")
L.append("1. Generate the 133 into `Expansion2.kt` as `CityInfo` records; add `fameTier`/`wave` + `postcard` fields to `CityInfo`.")
L.append("2. Postcard resolver in `CityPhoto()`: `country_<slug>` → `city_*` → `VgaCityCard`; log art-misses to a manifest.")
L.append("3. Replace the `expansionUnlocked` boolean with a **wave unlock table** (wave = tier); extend `GameData.ranks` with the International grades.")
L.append("4. In `newCase()`: draw the arrest from the player's current wave; keep `SpacedRepetition` for hops; cap first-sightings at `new/case`; apply the slack% + intro-case bump.")
L.append("5. Persist highest unlocked wave; reuse `visitedPlaces` for the \"recognized by N nations\" dispatch.")
L.append("")
L.append("## 12. Tunable knobs")
L.append("")
L.append("Wave boundaries & order · countries per wave · `new/case` cap · slack % ramp and floor · per-hop cost (`PERHOP`) · wrong-flight cost & error rates (§7) · cases per patent · free-career length. All are isolated constants; changing one re-derives the tables above.")
L.append("")

io.open("/home/deniswsrosa/Documents/projects/WhereIS/docs/05-game-design-and-progression.md","w",encoding="utf-8").write("\n".join(L))
print("wrote docs/05-game-design-and-progression.md :", len("\n".join(L)),"chars; waves",wave_ct,"art",art,"/",TOT)
