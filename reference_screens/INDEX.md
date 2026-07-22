# Original game screens — reference

These are the authentic screens from *Where in the World is Carmen Sandiego? (Enhanced, 1990)*,
extracted from the game's own `CARMEN##.BMP` assets and upscaled 2× (nearest-neighbour) for viewing.
Use these to check how close each Android screen is to the original.

| # / file | What it is | Android status |
|---|---|---|
| 01, 02 `anim_detective_walk_*` | Detective walking sprite frames (intro/travel animation) | Not used yet |
| 03–05 `anim_thief_run_*` | The thief/gang running sprite frames | Not used yet |
| **06 `title_screen`** | Title ("Where in the World…") | ✅ Matched (`title_screen` asset) |
| **07 `headquarters_signon`** | ACME HQ terminal, "identify yourself" | ✅ Matched (`hq_screen` asset); keyboard no longer covers the field |
| **08 `city_screen_with_description`** | City: photo (left) + description panel (right) + toolbar below | ✅ Matched — toolbar now full/uncut below a shorter panel |
| **09 `INVESTIGATE_witness_speech_bubble`** | Investigate a place: **witness bust + speech bubble** replaces the right panel; photo stays left | ✅ Matched — `WitnessPanel` (bust facing right, speech bubble, occupation label). Busts are procedural caricatures, simpler than the original's hand-drawn portraits |
| **10 `depart_travel_worldmap`** | DEPART: destination **list box (top-left)** over the world map with labelled cities | ⚠️ Different — Android shows a right-side destination panel, not a top-left list over a labelled map. Candidate to redo |
| 11–21 `city_*` | City screens for Reykjavik, Moscow, Paris, Buenos Aires, San Marino, Singapore, Bangkok, Kigali, etc. | ✅ Layout matched; 9 cities use real cropped photos, the other 21 use a procedural VGA "postcard" placeholder (CITIES.DAT photo codec still unsolved) |
| 12 `city_reykjavik_thief_arriving` | City screen with the thief sprite arriving | Thief arrival animation not implemented |

### Web reference screenshots (from abandonwaredos, in this folder too)
| file | what it is | Android status |
|---|---|---|
| `web_03_INVESTIGATE_3buildings_picker` | Investigate → **3 building icons + location names** (Museum/Library/Stock Exchange) | ✅ Matched — `InvestigatePicker` draws 3 civic buildings + names |
| `web_06_CRIME_COMPUTER` | Crime computer = **beige CRT monitor** with yellow SEX/HOBBY/HAIR/FEATURE/VEHICLE + COMPUTE, on a computer base | ✅ Matched — monitor bezel + dark screen + `drawComputerBase`; COMPUTE→matching suspects verified |
| `web_02/04_DEPART` | DEPART = destination list + world map with a **red route line** | ⚠️ Partial — DOS destination list ✅; map is a static asset (no dynamic route line — needs per-city map coords) |
| `web_05_DOSSIER_suspect_portrait` | Dossiers = **hand-drawn suspect portrait + stats** | ⚠️ Check — Android Dossiers overlay lists suspects; portraits are text/simple |

### Known gaps vs the original (remaining)
- **DEPART route line**: the original draws a red flight path between cities on the map; Android shows a static map. Needs per-city map pixel coordinates.
- **Witness portraits**: procedural busts vs the original's hand-drawn caricatures.
- **Sprite animations** (01–05, 12): detective/thief walk-run and arrival scenes not wired in.
- **21 city photos**: procedural placeholders until the CITIES.DAT image codec is cracked.

_Status legend: ✅ matched · ⚠️ partial/different · (blank) not started._
