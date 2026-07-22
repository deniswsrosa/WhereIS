# Where in the World is Carmen Sandiego? (Enhanced) — Extracted Corpus

## Edition (pinned from the binary)

- **Title:** Where in the World is Carmen Sandiego? (Enhanced)
- **In-binary version string:** `MS-DOS Version 2.1`
- **In-binary copyright:** `Copyright 1990, Bröderbund Software`
- **Compressor:** `PKLITE Copr. 1990 PKWARE Inc.`
- **Source:** [msdos_Where_in_the_World_is_Carmen_Sandiego_Enhanced_1989](https://archive.org/details/msdos_Where_in_the_World_is_Carmen_Sandiego_Enhanced_1989)
- **Note:** Multimedia 'Enhanced' build: Windows-3.x-format BMP city art + MIDI/digitized sound. Textually distinct from the 1985 IBM 1.0 build.

## Hashes (SHA-256)

- `d9d4aa3a199e5abbe5a93b5744c28392bddebc8cd090d3edc230811877dc7b91`  carmen_enhanced_1989.zip
- `b6c90177e91c0014d0a156808c51699b3a39a7c676ed823a29c608867247d92c`  CARMEN.EXE (compressed)
- `2d24be8520b991a44516e313a587468259d654ef5e57a97da137fba0cd57e788`  CARMEN.000.exe (decompressed)
- `847d5d8d5298e62ad7a8aa13d184edb123c9b3ce69f2e6516bbf4810ef76d283`  CITIES.DAT

- Internet Archive MD5 verified on download: `947eba22c5d8eedca917fe8381b2c708`

## Counts

- EXE byte-exact strings: **432**
- Cities (CITIES.DAT): **30**
- Suspects (structured dossiers): **10**

## Cities (CITIES.DAT, byte-exact, in file order)

- `0x000012` **Athens**
- `0x001417` **Baghdad**
- `0x002def` **Bamako**
- `0x003fa7` **Bangkok**
- `0x00560f` **Budapest**
- `0x006e71` **Buenos Aires**
- `0x0083e0` **Cairo**
- `0x009cc0` **Colombo**
- `0x00b135` **Istanbul**
- `0x00c636` **Kathmandu**
- `0x00dabd` **Kigali**
- `0x00f4c0` **Lima**
- `0x010eec` **London**
- `0x011df2` **Mexico City**
- `0x013047` **Montreal**
- `0x0140e5` **Moroni**
- `0x0151da` **Moscow**
- `0x01667a` **New Delhi**
- `0x017c00` **New York**
- `0x018e8a` **Oslo**
- `0x01a44c` **Paris**
- `0x01bc08` **Peking**
- `0x01d35b` **Port Moresby**
- `0x01e347` **Reykjavik**
- `0x01fb1c` **Rio de Janeiro**
- `0x021160` **Rome**
- `0x0229b6` **San Marino**
- `0x02412f` **Singapore**
- `0x025528` **Sydney**
- `0x0269be` **Tokyo**

> Note the outdated toponym **Peking** (not Beijing) — preserved verbatim.

## Suspect dossiers (CARMEN.EXE, byte-exact)

### Carmen Sandiego
- Sex: Female
- Occupation: Former spy for the Intelligence Service of Monaco
- Hobby: Tennis
- Hair: Reddish-brown
- Auto: 1939 Packard convertible
- Feature: Never appears in public without her ruby necklace.
- Other: Great fondness for tacos.

### Merey LaRoc
- Sex: Female
- Occupation: Freelance aerobic dancer
- Hobby: Mountain climbing
- Hair: Brown
- Auto: Fancy limousine
- Feature: Has an absolute mania for fancy jewelry.
- Other: Loves spicy foods.

### Dazzle Annie Nonker
- Sex: Female
- Occupation: Yogurt bar owner
- Hobby: Tennis
- Hair: Blond
- Auto: Bugatti Limousine
- Feature: Reported to have a tattoo.
- Other: Has an incredible craving for shellfish.

### Lady Agatha Wayland
- Sex: Female
- Occupation: Reader of upper-class English mystery stories
- Hobby: Tennis
- Hair: Red
- Auto: Denghby Roadster
- Feature: Has a diamond ring the size of a grapefruit.
- Other: Speeds through the countryside looking for great Mexican restaurants.

### Len "Red" Bulk
- Sex: Male
- Occupation: Ex-professional hockey player and gambler
- Hobby: Mountain climbing
- Hair: Red
- Auto: Convertible
- Feature: Tattoo of mermaid on his right thumb.
- Other: Loves seafood.

### Scar Graynolt
- Sex: Male
- Occupation: Folk guitarist
- Hobby: Croquet
- Hair: Red
- Auto: Limousine with shaded windows
- Feature: Wears a five-carat pinky ring.
- Other: Has a 6'8" man servant named "The Asp"; can eat his own weight in tacos.

### Nick Brunch
- Sex: Male
- Occupation: Ex-private eye
- Hobby: Mountain climbing
- Hair: Black
- Auto: "Black Mamba" motorcycle
- Feature: Prefers soiled trenchcoats and snap-brimmed fedoras.  Has brown eyes and a moustache.
- Other: Loves Mexican food; always wears Crimefighter's ring.

### Fast Eddie B.
- Sex: Male
- Occupation: World class croquet player
- Hobby: Croquet
- Hair: Raven-haired or black
- Auto: Convertible
- Feature: Always leaves a diamond stickpin at the scene of his crimes.
- Other: Fast Eddie is an impeccably dressed jet-setter and likes Mexican food.

### Ihor Ihorovich
- Sex: Male
- Occupation: Pretender to the Czarist throne
- Hobby: Croquet
- Hair: Blond
- Auto: Limousine
- Feature: Strange Ukranian tattoo on right shoulder.
- Other: Loves eating lobsters, watching cartoons and is fascinated by large marsupials.

### Katherine "Boom-Boom" Drib
- Sex: Female
- Occupation: Motorcycle racer
- Hobby: Mountain climbing
- Hair: Brunette or brown
- Auto: Honcho-1250 motorcycle
- Feature: Has a tattoo of an eagle on her left bicep.
- Other: Gourmet seafood cook; fascinated with health and fitness.

## Placeholder inventory (runtime-assembled templates)

Placeholders are C `printf`-style. `%Fs` is a far-pointer string. Assembly is inferred from
string layout, **not** runtime-verified (no DOSBox capture in this pass).

- `%.11Fs` × 1
- `%.12s` × 1
- `%c` × 2
- `%d` × 3
- `%s` × 4

## Preserved anomalies (NOT corrected)

- `tennis raquet` (misspelling of *racquet*) — witness fragment `0x1996e`
- `Ukranian` (misspelling of *Ukrainian*) — Ihor Ihorovich dossier
- `Peking` (outdated toponym) — CITIES.DAT
- `Bröderbund` with the German ö in the copyright line

## Section index (every EXE string is classified)

- **dos-runtime** — 43 strings (`0x019440`–`0x01c1d0`)
- **credits** — 10 strings (`0x0194a5`–`0x019544`)
- **title-copyright** — 1 strings (`0x0195a1`–`0x0195a1`)
- **intro** — 7 strings (`0x0195ce`–`0x019698`)
- **witness-clue-fragment** — 34 strings (`0x0196ba`–`0x019a22`)
- **attribute-token** — 21 strings (`0x019a47`–`0x019ae0`)
- **suspect-dossier** — 80 strings (`0x019aff`–`0x01a21d`)
- **ui-button** — 4 strings (`0x01a267`–`0x01a2a4`)
- **venue** — 12 strings (`0x01a2b2`–`0x01a313`)
- **occupation** — 38 strings (`0x01a324`–`0x01a4e5`)
- **no-information-response** — 14 strings (`0x01a4f3`–`0x01a7bb`)
- **hardware-error** — 4 strings (`0x01a7c7`–`0x01a81d`)
- **clue-lead-in** — 10 strings (`0x01a831`–`0x01a929`)
- **save-load-ui** — 12 strings (`0x01a93a`–`0x01aa3d`)
- **danger-message** — 7 strings (`0x01aa67`–`0x01ab73`)
- **crime-computer-label** — 7 strings (`0x01ab8c`–`0x01abb3`)
- **warrant-ui** — 2 strings (`0x01ac65`–`0x01accf`)
- **dossier-field-label** — 8 strings (`0x01acf5`–`0x01ad27`)
- **data-filename** — 6 strings (`0x01ad4d`–`0x01ada3`)
- **hardware-config** — 17 strings (`0x01adc0`–`0x01aecd`)
- **menu** — 12 strings (`0x01aed3`–`0x01af63`)
- **roster-name** — 12 strings (`0x01af71`–`0x01b060`)
- **rank** — 5 strings (`0x01b070`–`0x01b097`)
- **signon-interpol** — 9 strings (`0x01b13e`–`0x01b3f4`)
- **case-flow** — 12 strings (`0x01b492`–`0x01b9ca`)
- **misc-number-word** — 10 strings (`0x01b9ce`–`0x01ba29`)
- **pronoun-token** — 9 strings (`0x01ba2d`–`0x01ba5c`)
- **misc** — 11 strings (`0x01ba61`–`0x01bb0a`)
- **disk-swap-ui** — 14 strings (`0x01bb4d`–`0x01bbe5`)
- **save-filename** — 1 strings (`0x01bbed`–`0x01bbed`)

## Non-canonical / excluded files

- `ACME.DAT` — saved detective **roster/scores** (player names, GAME*.SAV refs). Player data, not game text.
- `DESKTOPD.CFG` — launcher config.
- `CARMEN.DAT` — graphics bank (no readable prose).
- `*.BMP` — Windows-3.x city/character art (320×200×8).
- `DIGISND.DAT`, `MIDISND.DAT` — audio.

## Negative findings (edition-specific)

- **No textual city descriptions / population passages / country names exist in this edition.**
  City knowledge is conveyed by the photographic city art (CITIES.DAT images), not prose.
- Witness clues describe the **suspect** (hobby/hair/feature/vehicle/food) for the crime computer,
  **not** the travel destination. The spec's 'Bamako ~800,000 population' text is absent here —
  it belongs to a different build or is rendered graphically.
