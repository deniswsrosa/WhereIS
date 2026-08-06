# Android Fidelity TODO — full findings from the 2026-07-24 dual playthrough

> **STATUS (2026-07-24 evening pass): §1–§18, §20, §21 implemented and verified on the
> emulator against the bundled refs.** Notes from the fix pass:
> - §5: ACME scene shipped as `intro_acme_agency.png` (cursor patched with a brick block
>   cloned from 64 px below; header/footer text is baked into the capture).
> - §6: sprites shipped as `sight_face/thug/burglar_peek/burglar_run/dagger.png` (crops
>   from the refs at ÷2; black background kept — invisible on the black panel). Hideout is
>   now venue-based: `hideoutVenueIndex` in the ViewModel, wrong venue = dagger + forced
>   "Rumor has it…" line, right venue = chase. Arrival no longer auto-confronts.
> - §9: `jail_cell.png` proved to be an exact pixel match of the eyes-B frame at offset
>   (63,71); frame A shipped as `jail_eyes_alt.png` and alternated every 1 s.
> - §10: the "airport photo" is just the DEPARTURE CITY's photo staying behind the map —
>   fixed by keeping CityPhoto during flight (no new asset needed).
> - §15: **the doc had this inverted** — the DOS refs show the area below the right panel
>   is WHITE on the printer screens (sign-on AND result), and `hq_screen.png` already is.
>   Only the button geometry was fixed (Yes 152/76, No 234/76, h≈12, drop shadow).
> - SEE/DEPART now share one stable per-city `departOptions` list (SEE list == DEPART
>   list, generated once per arrival); DEPART kept tap-outside-to-cancel instead of the
>   invented "Cancel" row, and got the DOS grow-out animation.
> - Still open: §19 clue-phrasing audit (separate content/balance pass), §17's optional
>   LED blink, and section D (sound, menu dropdown contents).
>
> **STATUS (2026-07-24 later pass): remaining gaps closed.**
> - §17 LED blink: front-bezel LEDs (`crime_computer_leds.png`, cropped from
>   `dos_computer_wait_led.png` at virtual (60,97)) blink every 350 ms during COMPUTE.
> - §19 clue phrasing: replaced invented templates with the DOS EXE grammar —
>   `GameData.traitClueFragments` (verbatim per-trait sentences, 3 each / 1 for hair) with
>   `{S}/{s}/{p}` pronoun slots filled from the culprit's sex (so a clue leaks the sex the
>   way the original does), the 8 real `clueLeadIns`, and `noInformationByVenue` (per-venue
>   off-track apology). Destination clues cite a region + landmark fact via CityMeta instead
>   of naming the city. Verified on the emulator (trait, destination, off-track lines).
> - Menus: Game = About Carmen… / New / **Save (grayed)** / **Quit** (→ "Do you really want
>   to quit?" Yes/No dialog); Options = √Sound / Joystick; Dossiers uses the EXE short names.
> - Riverfront art: already present at `work/identified/venues/riverfront.png` and wired as
>   `venue_riverfront.png` (no capture needed).
> - Sound: `MIDISND.DAT` is the CARMEN.DAT container format holding 12 SMF sequences; item 11
>   is the full-arrangement title theme, extracted to `res/raw/theme.mid` and played by
>   Android's Sonivox GM synth (`audio/GameSound.kt`) on intro/title/sign-on, stopping on
>   gameplay, honoring Options > Sound (fake "not wired up" dialog removed). The 11 event
>   stingers are bundled as `jingle_0..10` but not yet mapped to events — the correct
>   event→stinger mapping needs auditioning (a wrong guess would regress fidelity).
>
> **CITIES.DAT text (not cracked):** its per-item text is bit-packed with a variable-length
> code keyed off the EXE's frequency-ordered alphabet at file 0x1bd66 (LZSS-for-images and
> nibble-escape both fail on it). The live-memory-dump fallback (drive DOSBox-X to a witness,
> read guest RAM) is blocked by the packaged `dosbox-x` carrying `cap_net_raw=ep`, which makes
> the process non-dumpable; a cap-stripped copy is dumpable but the Xwayland driving proved
> too flaky to complete a run this session. Destination clues use CityMeta facts instead.

Both games were played end-to-end to a **win** on the same day and compared stage by stage:

- **DOS** (ground truth): case "antique gaucho costume stolen from Buenos Aires", suspect
  Merey LaRoc, route Buenos Aires → Bamako → Peking → Lima → Montreal (hideout). Warrant
  issued, arrest chase, jail, report, promotion quiz answered → new rank Sleuth.
- **Android**: case "Crown Jewels stolen from Budapest", suspect Len "Red" Bulk, route
  Budapest → Moscow → Mexico City → San Marino → Sydney → Kathmandu (hideout). Warrant,
  chase, jail, quiz (failed due to a bug, see §20).

Every claim below was observed live. Reference screenshots for each item are in
**`reference_screens/fidelity_2026-07-24/`** — files prefixed `dos_` are the ground truth
(640×400 window grabs = exactly 2× the 320×200 virtual canvas; divide pixel coordinates by 2
to get virtual coordinates). Files prefixed `android_BAD_` show the current wrong Android
rendering (2400×1080 emulator screenshots).

The guiding rule (persisted in memory): **fidelity over invention** — a player of the DOS
original must not notice a difference. When in doubt, re-capture from DOSBox and measure.

---

## Code map (where things live today)

| Area | File |
|---|---|
| All screens (Intro, Title, HqPrinter/SignOn/Briefing, City, Toolbar, InvestigatePicker, WitnessPanel, Crime, Travel, Chase, Result) | `android/app/src/main/java/com/acme/carmen/ui/screens/GameScreens.kt` |
| Game state machine, clocks, travel, warrant, sighting flags | `android/app/src/main/java/com/acme/carmen/game/CarmenViewModel.kt` |
| Text corpus, suspects, venues, quiz | `android/app/src/main/java/com/acme/carmen/data/GameData.kt` |
| VirtualScreen (320×200 coordinate system, `v.At(x,y,w,h)`, `v.text(px)`, IME pan) | `android/app/src/main/java/com/acme/carmen/ui/CommonUi.kt` |
| Menu bar + Dossiers windows | `android/app/src/main/java/com/acme/carmen/ui/GameMenuBar.kt` |
| Assets (PNG, native resolution, no scaling) | `android/app/src/main/res/drawable-nodpi/` |
| Phase routing | `android/app/src/main/java/com/acme/carmen/MainActivity.kt` |

Build/deploy/run:

```bash
cd android && JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug \
  && adb install -r app/build/outputs/apk/debug/app-debug.apk \
  && adb shell am start -n com.acme.carmen/.MainActivity
```

Emulator driving (Pixel_6_API_34, 2400×1080): canvas left≈502, top≈68, ≈4.77 real px per
virtual px. Useful taps: toolbar y=912 with SEE≈1302 / DEPART≈1510 / INVESTIGATE≈1717 /
CRIME≈1925; crime rows x=1416, y=206 (SEX) /256 (HOBBY) /304 (HAIR) /352 (FEATURE) /394
(VEHICLE), COMPUTE=488; depart list items x=880, y=220/277/336/392; venue-picker name rows
x=1266, y=750/800/850. Debug: `adb logcat -d | grep Carmen` prints each case's culprit+route.

DOSBox ground-truth rig (Linux): `Xwayland :9 -geometry 1280x800 -retro -noreset &`, then
`DISPLAY=:9 dosbox-x -conf tools/carmen-linux.conf` from the repo root. Mouse is dead to the
guest — drive with `DISPLAY=:9 xdotool key --window $WID Tab/Return/Escape/Up/Down/space`
(Tab cycles toolbar; Up/Down wrap; Return activates/cycles; Esc leaves the computer; after a
witness, Space re-activates the currently selected tool). Capture:
`DISPLAY=:9 import -window $WID png:- | convert - -crop 640x400+0+17 +repage out.png`
(the +17 crop removes the DOSBox menu bar — do not skip it, it shifts all measurements).

---

## A. Invented UI that must be replaced or deleted (worst offenders)

### 1. SEE tool — replace the blue modal with the DOS "HIDE" dropdown
- **Android today**: tapping SEE opens a full-blown invented modal: royal-blue rectangle,
  yellow city title, the country description text, and a green "CLOSE" button
  (`android_BAD_see_blue_modal.png`). Nothing remotely like this exists in the original.
- **DOS truth** (`dos_see_dropdown_open_hide_icon.png`): activating SEE *replaces the
  city/date box* (top-left, virtual ≈ At(4,13,143,24)) with a taller black dropdown, white
  double border, containing: line 1 = current city name (white, centered, bold), then one
  line per connection city (3 cities in the observed case). Selected row = white bar with
  black text (keyboard Up/Down moves it, wraps). The rest of the screen (photo, right panel,
  toolbar) stays put. **The SEE icon's label changes to "HIDE"** while the list is open
  (green selection border stays on it); activating again (or Return) closes the list and the
  label reverts to SEE. After closing, the right panel still shows the country text
  (`dos_see_after_hide_country_text.png`).
- Measured from the capture (640×400 → virtual): dropdown spans ≈ x 8..286 px real → virtual
  (4,13) to (143, ~67); ≈10 virtual px per row; same double-border style as the city box.
- Implementation notes: this is a CityScreen overlay state, not a Dialog. Add
  `seeOpen: Boolean`; when open draw the dropdown box in place of the city box and render
  `vm.connections(currentCity)` (SEE shows the DEPART destination set — in DOS the SEE list
  content equalled the depart list minus nothing we could distinguish; use the same list the
  DEPART screen uses). Toolbar label swap needs the SEE ToolZone to render text "HIDE"
  when open. Tap on the SEE/HIDE zone toggles; tapping a row may also just close (DOS
  keyboard Return closes; mouse behavior unverified — keep it simple: any tap closes).

### 2. Delete the "WARRANT: <name>" banner on the city photo
- Android draws a yellow-on-black banner over the bottom of the city photo once a warrant
  exists (`android_BAD_warrant_banner.png`). **DOS has no such thing anywhere.** The warrant
  only ever appears as printer text. Remove the composable entirely (CityScreen).

### 3. Delete the green ▶ indicator in the witness panel
- Bottom-right green triangle "continue" hint (`android_BAD_witness_layout_green_arrow.png`).
  Doesn't exist in DOS. Remove it (WitnessPanel in GameScreens.kt).

### 4. Stop accumulating old routes on the DEPART/flight map
- Android keeps every previous leg as red dashes plus an unlabeled yellow marker at each
  previously visited city, on every later DEPART screen and flight
  (`android_BAD_stale_routes_on_depart.png` — shows Budapest→Moscow→Mexico City chains while
  departing San Marino). **DOS draws a clean map every time**
  (`dos_depart_list_first_preselected.png`): only current city (white label + small white
  marker) and the 3-4 destination cities (yellow labels + small yellow markers). During the
  flight animation only the single current leg grows (`dos_flight_route_growing.png`).
- Fix: the map composable must render only (a) current city, (b) offered destinations, and
  during TRAVEL only the in-progress leg. Clear/route state lives in CarmenViewModel
  (`travelTo`/`arrive`); the map should not read a persistent route history. (DOS label
  quirk kept: labels near the right map edge flip to the left of their marker — SYDNEY did
  this in both games, so current flip logic is correct.)

---

## B. Missing DOS screens & animations

### 5. Intro sequence: wrong composition + missing final ACME scene
- **DOS order** (attract loop, ~3-4 s per stage, each on its own screen):
  1. Title photo screen ("PRESS ANY KEY TO BEGIN" flashing yellow, bottom center).
  2. Three crowns + "Brøderbund Software Presents" — **alone, nothing else on screen**
     (`dos_intro_crowns_alone.png`).
  3. Solid black screen, detective sprite walks across near the bottom (small, ~40 px tall
     virtual; `dos_intro_detective_on_black.png`).
  4. Solid black screen, the three-cops group marches across
     (`dos_intro_cops_on_black.png`).
  5. **ACME Detective Agency scene — currently missing from Android entirely**
     (`dos_intro_acme_agency_scene.png`): left half = city street with HOTEL sign and
     building "527", right half = brick wall with a big window "ACME DETECTIVE AGENCY" and
     office interior (desk, fan, plant). Header text (white on black, centered):
     "Carmen's gang has pulled another caper!" Footer: "and it's up to you to crack the
     case…". Then the loop returns to the title.
- **Android today**: detective and cops walk *over* the crowns/text screen simultaneously,
  and stage 5 doesn't exist.
- Fix in IntroScreen (GameScreens.kt): sequence the stages on separate black screens with
  the existing `anim_detective_*` / `anim_cops_*` sprites, and add the ACME scene. The scene
  art must be captured from DOSBox (it shows during the attract loop ~12-16 s after boot,
  right before returning to title; also plays after "PRESS ANY KEY" delay). Capture at
  640×400, downscale to 320×200, save as `drawable-nodpi/intro_acme_agency.png`. The frame
  in `reference_screens/fidelity_2026-07-24/` has the X11 cursor baked at ≈(325,213) real —
  either re-capture with the pointer parked in a corner (`DISPLAY=:9 xdotool mousemove 5 5`)
  or patch those ~16×16 px from a second capture.

### 6. Sighting animations: play them during INVESTIGATE, with DOS's escalation ladder
This is the single biggest *feel* gap. DOS plays an interstitial animation in the black
right panel when you pick a venue, **before** the witness pops up, and the animation
escalates as you close in on the suspect:

| Distance from suspect | DOS animation | Reference |
|---|---|---|
| trail, mid-distance | Small masked face (green cap) rises slowly from the panel's bottom edge, pauses, sinks back | `dos_sighting1_henchman_face_rising.png` (Bamako) |
| closer (next city) | Bigger striped-shirt thug pops up, arms raised, shakes | `dos_sighting2_thug_arms_up.png` (Peking) |
| one city before hideout | Burglar with loot sack peeks in from the RIGHT edge, then runs across the panel right→left | `dos_sighting3_burglar_sack_peek.png`, `dos_sighting3_burglar_sack_running.png` (Lima) |
| hideout city, wrong venue | A dagger flies across / sticks in the panel (danger warning), then the witness says the special line "Rumor has it that the gang is in town somewhere." | `dos_hideout_dagger_thrown.png`, `dos_hideout_rumor_witness.png` (Montreal) |

- **Android today** shows one static thug sprite (anim_burglar) in the right panel *on
  arrival* in a city near the end of the route, and nothing venue-related. That's the wrong
  trigger and no escalation.
- Fix:
  - ViewModel: replace the boolean `sighting` with a distance metric
    (`route.size-1 - progressIndex`) and expose it to the venue-visit flow
    (`openVenue`). The animation should run inside the witness-panel flow: venue selected →
    footsteps → **sighting anim (if distance ≤ 3)** → witness bubble.
  - Hideout city: wrong venue → dagger anim + force the witness line to the "Rumor has it
    that the gang is in town somewhere." text (add to GameData if missing); right venue →
    Phase.CHASE as today.
  - Sprites needed (extract from the reference PNGs above at 2×, downscale ÷2, transparent
    background): masked rising face (~30×28 virtual), thug-arms-up (~34×40), burglar+sack
    2-3 run frames (~40×40), dagger (~34×8). The existing `anim_burglar.png` (44×34) is the
    arms-up thug — reuse where it fits.
  - Remove the arrival-time thug display in CityScreen.

### 7. "SLEEPING…" overnight state in the city/date box
- When an action pushes the clock past 10 p.m., DOS swaps the city-box title to
  **"SLEEPING…"** with the advancing date/time line under it until 8 a.m.
  (`dos_sleeping_overnight.png` — captured mid-compute in Bamako: title literally
  "SLEEPING… / Tuesday, 8 a.m."). Android silently clamps the clock with no feedback.
- Fix: in the clock-advance path (CarmenViewModel `arrive`/`openVenue`/`compute`), when the
  overnight clamp fires, set a transient `sleeping=true`; the city box composable renders
  "SLEEPING…" instead of the city name for ~1.5-2 s (with the new morning time), then
  reverts. DOS shows it during the computer session too (the CRT stays up).

### 8. Arrest chase: hide the toolbar
- During the whole chase sequence (suspect run → "There goes the suspect!" → cops → escort)
  DOS **removes the toolbar completely** — the area below the right panel is plain black
  (`dos_chase_suspect_no_toolbar.png`, `dos_chase_there_goes_text.png`,
  `dos_chase_cops.png`). Android's ChaseScreen currently draws a static toolbar with
  INVESTIGATE selected. Remove it (ChaseScreen in GameScreens.kt) and let the panel sit on
  black.
- While verifying, also match the text frame: "There goes\nthe suspect!" is centered in the
  panel, white, plain font (already close).

### 9. Jail: blinking eyes animation
- The DOS jail scene animates: the suspect's eyes at the top of the barred window
  open/close on a slow loop (compare `dos_jail_eyes_a.png` vs `dos_jail_eyes_b.png` —
  eye white visible vs. not). Android's `jail_cell.png` is static.
- Fix: extract the two eye states from the refs (the differing region is only a few px at
  the window top, virtual ≈ (222..232, 40..46) — measure from the two files), ship
  `jail_eyes_open/closed` overlays or a second full frame, and alternate every ~1 s in
  ResultScreen while the jail is visible.

### 10. Flight screen: keep the airport/plane photo behind the map
- During DOS travel, the area top-left behind the map (above which the ticking city/date box
  sits) shows a **photo scene — a plane at the gate / airport** — not a sky gradient
  (`dos_flight_airport_photo_behind_map.png`, `dos_flight_route_growing.png` shows trees /
  airport at a different leg). Android draws a plain blue gradient there.
- The photo appears to be a static departure-airport scene (same one each flight leg we
  observed). Capture it cleanly from a DOS flight (it's partially covered by the map; the
  visible strip is only ~145×65 virtual at top-left). Since only the strip above the map is
  visible, a cropped strip asset is enough.

---

## C. Behavior / rules mismatches

### 11. Promotion quiz must allow retries
- DOS on a wrong answer prints **"That is incorrect. Please try again."** and re-asks the
  *same* question, keeping the promotion attainable (`dos_quiz_incorrect_try_again.png` —
  "Kentucky" rejected, then "Virginia" accepted → "Your new rank is: Sleuth. / Four more
  cases until your next promotion." `dos_quiz_correct_new_rank_sleuth.png`).
- Android prints "I'm sorry, that is not correct. / Your promotion will have to wait," and
  forfeits (GameScreens.kt line ~1173, `resolvePromotion`).
- Fix: loop until correct in ResultScreen (re-prompt, keep the answer field), match the DOS
  strings exactly: `That is incorrect.` / `Please try again.` then re-print the question.
  Whether DOS caps retries is unverified — infinite retry is the safe reading of what we
  saw (it re-asked immediately with no penalty).
- **Data fix**: our `promotionQuiz` in GameData does *not* contain the Iceland question —
  if it's ever added, the DOS-correct answer is **Virginia** (not Kentucky) for "Iceland is
  39,769 square miles; the size of the state of ________. (See Iceland, Geography: Area)".
  Worth auditing the other 11 invented pairs against the 1990 World Almanac for
  plausibility.

### 12. Toolbar selection must track the active tool
- DOS: the green selection border always sits on the tool you used last; activating a tool
  moves it; **after arriving in a new city the selection is INVESTIGATE**
  (`dos_city_investigate_selected_on_arrival.png`); after closing the computer it stays on
  CRIME; keyboard Space re-activates the selected tool.
- Android: selection is stuck on SEE regardless of what the player taps (tap-activation
  doesn't update the `selected` state; see GameToolbar/CityScreen wiring).
- Fix: single `selectedTool` state in the ViewModel; every activation sets it; `arrive()`
  resets it to INVESTIGATE.

### 13. Time costs
Observed DOS costs in this run:
- venue first visit: **2 h** (9 a.m. → 11 a.m.) — Android matches (2 h).
- venue re-visit (same or another venue after first): **3 h** (11 a.m. → 2 p.m.) — Android
  charges 2 h flat / free re-visit of same venue. (Memory note says re-visits free in app.)
- crime computer COMPUTE: **3 h** (2 p.m. → 5 p.m., and again 10 p.m. → next morning
  crossing SLEEP) — Android charges 1 h.
- flights: DOS legs observed 3 h (short) with 1-h map ticks; overnight arrivals land at
  8-9 a.m. (Android clamps to 8 a.m. — close enough, keep).
Fix in CarmenViewModel (`openVenue`, `compute`): first-visit 2 h, re-visit 3 h, compute 3 h.
(If exactness matters later, more DOS sampling is needed — these were consistent within this
run but n=1 per case.)

### 14. Depart list: pre-select the first city
- DOS opens the list with the **first destination already on the white selection bar**
  (`dos_depart_list_first_preselected.png`). Android shows no selection until a tap.
- Also verify row order: DOS list = [return city you came from? no —] in Bamako the list was
  [Buenos Aires, Peking, Rome, Sydney] (return city first), in Lima [Paris, Istanbul,
  Peking, Montreal]. Android puts destinations only. Unverified whether DOS always lists the
  arrival-from city — leave as-is unless new evidence.

### 15. Sign-on screen: black background behind buttons/text
- In DOS, everything outside the two panels is **black**; the Yes/No buttons and the
  "Press any key or button to continue." bold text sit on black
  (`dos_signon_yesno_black_background.png`). Android renders a **white strip** below the
  right panel during sign-on (Yes/No and press-any-key sit on white). ResultScreen already
  does it right (black). Fix HqPrinterScreen's lower-right region.
- DOS button geometry (÷2 from the 640 capture): Yes ≈ At(152,176,76,11), No ≈
  At(234,176,76,11) — yellow fill, black 1px border with small drop shadow, red bold
  centered text. Android's are close but taller; match height ~11-12 virtual px.

### 16. Witness panel layout: sprite and bubble side-by-side, vertically centered
- DOS (`dos_witness_waiter_layout.png`, `dos_witness_bartender_layout.png`): the witness
  sprite sits at the panel's left edge, roughly vertically centered (its cap-label e.g.
  "WAITER" directly under the sprite, not at the panel bottom), and the speech bubble sits
  to its RIGHT, vertically centered against the sprite, tail pointing left into the sprite.
  Sprite is large: the waiter is ≈55×95 virtual (≈ 1/3 of panel width, over half its
  height).
- Android: bubble pinned to the panel top, sprite pinned to the bottom-left far below it,
  occupation label at panel bottom, big dead gap between them; sprite rendered at width 32
  virtual (too small vs DOS).
- Fix WitnessPanel: lay out as a vertically-centered Row: [sprite+label column] [bubble],
  sprite width ≈ 42-55 virtual depending on native aspect (measure per-sprite against DOS
  captures; the DOS sprites vary in size — ours were extracted from the same art so native
  pixel size ÷2 should equal DOS's on-screen size — render at native 1:1 virtual pixels
  instead of forcing width 32).

### 17. Crime computer polish
- Android prints a trailing **"READY."** after the warrant/suspect list; DOS does not (it
  only prints READY. at the start of a computer session)
  (`dos_computer_wait_led.png` sequence). Remove the trailing line (CrimeScreen
  `runCompute`).
- Cosmetic (optional): while computing, DOS blinks the LEDs on the computer's front bezel
  (`dos_computer_wait_led.png` vs `dos_computer_initial.png`) — a 2-frame overlay swap
  during the "Wait…" period.
- Value cycling orders observed in DOS (for reference, first press → …): VEHICLE:
  convertible → limousine → race car → motorcycle → (blank); HAIR first press → brown.
  Android's cycle orders come from GameData lists — align if they differ.
- After compute finishes, DOS leaves **no row selected** (all yellow) until the next
  keypress; Android keeps COMPUTE on the white bar. Minor; align if cheap.

### 18. Title screen: accept a tap anywhere
- DOS advances on any key/button. Android's TitleScreen only advances when the
  "PRESS ANY KEY TO BEGIN" strip itself is tapped (`v.At(0,176,320,24)` clickable). Make the
  whole screen clickable (GameScreens.kt TitleScreen line ~71).

### 19. Witness clue phrasing (content audit — lower priority)
- DOS lines observed: "All I know is that she planned to drive a dune buggy over the
  Sahara. She had brown hair." (two facts in one line), "I saw the person you're looking for
  and she planned to visit Minya Konka. She arrived in a private limo.", "I saw the person
  you're looking for and she planned to take a census of birds of the Northwest
  Territories." Hideout: "Rumor has it that the gang is in town somewhere."
- Android uses different templates: "I heard he was headed for a city in Europe, known for
  Red Square." / "A suspicious person was here and he was headed for…" / "A reliable source
  told me he was a mountain climber." / "My sources tell me the suspect arrived in a
  convertible."
- The DOS "known for X" phrasing isn't what the original says — the original names the
  actual destination attribute (Minya Konka, the Sahara, Northwest Territories birds) and
  often merges a suspect clue into the same sentence. If the corpus extraction
  (work/game_reference) has the real clue tables, swap them in; otherwise capture more DOS
  witness lines. This changes gameplay difficulty, so treat as a separate pass.

### 20. BUG: taps leak into the promotion-quiz text field
- Repro (this run): tapping to skip through chase/result stages while the quiz stage was
  being reached left "vv" in the answer BasicTextField → submitted answer became
  "vvMoroni" → wrongly judged incorrect (`android_BAD_quiz_vv_leak_and_forfeit.png`).
  Screen taps landing on the soft keyboard's keys while stages auto-advance get committed
  into the field.
- Fix: don't focus the answer field / don't show the IME until the quiz question has fully
  printed AND the previous stage's tap has been consumed; clear the field's text when the
  quiz stage begins; ignore Enter while empty.

### 21. Rookie route length (rules nit)
- DOS Rookie case: 5 cities total (4 hops). Android generated 6 cities. If newCase() draws
  route length independent of rank, align Rookie to 4 hops (check DOS behavior across more
  cases before hard-coding; 5 total was n=1).

---

## D. Previously-known gaps still open (from memory, re-confirmed)

- **No sound** anywhere (DOS has PC-speaker beeps for teletype, menus, chase).
- **Menu bar Game/Options/Acme dropdown contents** unverified against DOS (Dossiers is
  done). Capture each menu in DOSBox and mirror.
- ~~Travel drop-down grow animation~~ — **done in 02e38e2**, this bullet was stale. Verified
  in `TravelScreen` (GameScreens.kt): a `grow` state animates 0→1 via `LaunchedEffect`
  and drives the destination box's height (`v.At(4, 13, 141, 24f + (fullH - 24f) * grow)`),
  growing it out of the city box exactly as the changelog note above describes. (The SEE
  dropdown, `SeeDropdown`, still pops instantly with no grow — unconfirmed either way
  against DOS since reference captures are single static frames and can't show animation
  timing; not in scope here.)

## Suggested priority order for the next session

1. §1 SEE/HIDE dropdown (worst invented UI, small change).
2. §2, §3, §4 deletions (fast wins, pure fidelity).
3. §6 sighting ladder during INVESTIGATE + hideout dagger (big feel win; needs sprite
   extraction from the bundled refs).
4. §5 intro fix + ACME scene (needs one DOSBox capture session).
5. §8 chase toolbar, §12 toolbar selection tracking, §14 depart pre-select, §15 sign-on
   black strip, §16 witness layout (medium Compose edits).
6. §11 quiz retry + §20 input-leak bug (correctness).
7. §7 SLEEPING…, §9 jail blink, §10 airport strip, §13 time costs, §17 computer polish,
   §18 title tap, §21 route length.
8. §19 clue-phrasing audit last (content, affects balance).

After each fix: build, install, and eyeball against the matching `dos_*.png` reference at
the same game moment; for animations, screen-record the emulator
(`adb shell screenrecord`) and compare frame-by-frame with the DOS capture rig.
