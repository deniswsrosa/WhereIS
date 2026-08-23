# Paid story asset generation manifest

- Tool mode: built-in `image_gen` (project-bound outputs copied into this directory; generated originals retained in the tool output directory)
- Generated for: review mockup only; not yet wired into Android sprite lookup
- Shared references:
  - `android/app/src/main/assets/sprites/suspects/suspect_clara_san_diego.png`
  - `android/app/src/main/assets/sprites/screens/jail_cell.png`
  - `android/app/src/main/assets/sprites/screens/world_map_clean.png`
  - `android/app/src/main/assets/sprites/cities/city_santorini.png`
- Technical review: all six scenes remain readable at a 144×108 preview; all five stamps have genuine alpha transparency and remain distinct at 48×48.

## Exact final prompts

### `escape_a_runway.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: reusable “Clara escaped” story illustration, candidate A.
> Use: displayed in the right-hand art panel beside a separately rendered typed case report after Case 14 and later story finales.
> Subject: Clara, matching the supplied portrait’s defining red wide-brim hat, red hair, tan trench coat, and guarded expression, seen in three-quarter rear view sprinting toward a small propeller aircraft on a night runway; one hand holds her hat, coat tails sweep behind her, a distant city skyline and runway lights establish escape and international pursuit.
> Composition: landscape 4:3, one clear large silhouette centered-right, strong empty dark area along the left edge for UI separation, no speech bubble, no frame.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard 1-pixel dark outlines, limited 16-color palette, sparse ordered dithering, same visual era and palette discipline as the supplied game references; readable when reduced to roughly 144x140 pixels.
> Lighting: deep navy night, cyan runway lights, red/tan Clara silhouette, small warm aircraft cabin lights.
> Constraints: no words, no letters, no numbers, no logo, no watermark, no photorealism, no smooth vector gradients, no modern high-resolution painting, no antialiasing. Produce a clean rectangular RGB PNG.

### `escape_b_night_train.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: reusable “Clara escaped” story illustration, candidate B.
> Use: right-hand art panel beside a separately rendered typed case report after Case 14 and later story finales.
> Subject: Clara, matching the supplied portrait’s red wide-brim hat, red hair, tan trench coat and poised expression, stepping through the open door of a midnight international sleeper train as it begins to move; she looks back over one shoulder, holding a slim coded case file, with steam, platform lamps and a distant detective silhouette creating pursuit.
> Composition: landscape 4:3, Clara large and readable on the right half, train door frames her, left side dark and relatively quiet for strong separation from report UI; no speech bubble, no frame.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard dark outlines, limited 16-color palette, sparse ordered dithering, same visual era and palette discipline as supplied references; readable around 144x140.
> Lighting: black and deep navy station, cyan platform lamps, red/tan Clara, warm yellow train window.
> Constraints: no words, letters, numbers, logo, watermark, photorealism, smooth gradients, modern painting, or antialiasing. Clean rectangular RGB PNG.

### `escape_c_speedboat.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: reusable “Clara escaped” story illustration, candidate C.
> Use: right-hand art panel beside a separately rendered typed case report after Case 14 and later story finales.
> Subject: Clara, matching the supplied portrait’s red wide-brim hat, red hair and tan trench coat, already aboard a small speedboat leaving a shadowed harbor; she stands at the stern facing back toward the viewer, fingertips touching the hat brim, while a red scarf and wake show speed; a detective reaches the dock a moment too late and a moonlit foreign skyline recedes behind.
> Composition: landscape 4:3, Clara as the dominant clear silhouette on the center-right, diagonal white/cyan wake, quiet dark upper-left area, no speech bubble or border.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard 1-pixel dark outlines, limited 16-color palette, sparse ordered dithering, same visual era and palette discipline as supplied game references; readable around 144x140.
> Lighting: near-black harbor, cyan moon reflection and wake, red/tan Clara, a few warm yellow windows.
> Constraints: no words, letters, numbers, logo, watermark, photorealism, smooth gradients, modern painting, or antialiasing. Clean rectangular RGB PNG.

### `capture_a_jail.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: unique “Clara finally captured” story illustration, candidate A.
> Use: the one bespoke right-hand finale art panel shown only after Wave 10, beside separately rendered case-closed text.
> Subject: Clara, unmistakably matching the supplied red wide-brim hat, red hair and tan trench coat, seated calmly inside a World Detective Bureau jail cell behind thick bars; her red hat hangs from one bar in the foreground, hands visibly secured in front, expression defeated but dignified; five small sealed case folders sit outside the cell as a visual sign her five crime families are dismantled.
> Composition: landscape 4:3, Clara large at center, bars form a strong frame, hat in foreground, no printed labels or readable text.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard dark outlines, strict limited 16-color palette, sparse ordered dithering; closely harmonize with the supplied jail and Clara sprites; readable around 144x140.
> Lighting: black cell, gray brick and steel, focused warm light on Clara, red hat as focal accent, small cyan evidence-room light.
> Constraints: no words, letters, numbers, logo, watermark, photorealism, smooth gradients, modern painting, or antialiasing. Clean rectangular RGB PNG.

### `capture_b_evidence_room.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: unique “Clara finally captured” story illustration, candidate B.
> Use: the one bespoke finale art panel after Wave 10, beside separately rendered case-closed text.
> Subject: a dramatic evidence-room tableau: Clara matching the supplied red hat, red hair and tan trench coat stands in handcuffs at center while two dark-uniformed World Detective Bureau agents flank her; behind them is a large simplified world map with five red case strings all ending at one central pin; her closed red umbrella and travel case rest on the floor.
> Composition: landscape 4:3, triangular centered group, large readable Clara silhouette, map as simple graphic backdrop, no labels or readable text, no border.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard dark outlines, strict limited 16-color palette, sparse ordered dithering; same visual era and palette discipline as supplied sprites; readable around 144x140.
> Lighting: dim navy evidence room, cyan map light, warm face tones, red hat/string accents.
> Constraints: no words, letters, numbers, logo, watermark, photorealism, smooth gradients, modern painting, or antialiasing. Clean rectangular RGB PNG.

### `capture_c_airport.png`

> Create one project-bound game asset candidate for a 1990 DOS detective adventure.
>
> Asset: unique “Clara finally captured” story illustration, candidate C.
> Use: the one bespoke finale art panel after Wave 10, beside separately rendered case-closed text.
> Subject: dawn at an international airport: Clara matching the supplied red wide-brim hat, red hair and tan trench coat has been caught at the foot of an aircraft stairway; a detective closes one handcuff while Clara looks back with a wry composed expression; five passport-like evidence stamps lie fanned on an open case in the foreground, aircraft and sunrise behind.
> Composition: landscape 4:3, Clara and detective large center-right, open evidence case low-left, clear final-stop silhouette, no speech bubble, no frame.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art, chunky deliberate pixels, hard dark outlines, strict limited 16-color palette, sparse ordered dithering; same visual era and palette discipline as supplied game references; readable around 144x140.
> Lighting: deep violet dawn, cyan runway, pale yellow horizon, red hat and tan coat focal colors.
> Constraints: no words, letters, numbers, logo, watermark, photorealism, smooth gradients, modern painting, or antialiasing. Clean rectangular RGB PNG.

### `stamp_europe.png`

> Europe stamp: deep red and pale cream seal; central emblem combines a simplified castle arch and old key; tiny route arcs over a simplified European land silhouette. Create one project-bound game UI asset for a 1990 DOS detective adventure.
>
> Asset family: one of five crime-family route-piece stamps, used as a small earned seal on the painted Passport/world-route dossier after a regional mastermind capture.
> Shared composition: a compact roughly circular case-file seal, jagged perforated pixel edge, bold central regional emblem, one curving dotted travel route ending in a location pin, thick dark outline, no rectangular background, no shadow outside the seal.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art; chunky deliberate pixels; strict limited 16-color palette; hard dark outline; sparse ordered dithering; legible at 32–48 pixels; harmonize with the supplied world map’s white/cyan/navy palette.
> Output: square 1:1 PNG with genuine alpha transparency outside the irregular stamp silhouette (not a checkerboard pattern).
> Constraints: no words, no letters, no numbers, no flag, no logo, no watermark, no photorealism, no smooth vector gradients, no antialiasing.

### `stamp_americas.png`

> Americas stamp: bright cyan and pale cream seal; central emblem combines a simplified compass rose and wing; a route sweeps from north to south over a simplified two-continent silhouette. Create one project-bound game UI asset for a 1990 DOS detective adventure.
>
> Asset family: one of five crime-family route-piece stamps, used as a small earned seal on the painted Passport/world-route dossier after a regional mastermind capture.
> Shared composition: a compact roughly circular case-file seal, jagged perforated pixel edge, bold central regional emblem, one curving dotted travel route ending in a location pin, thick dark outline, no rectangular background, no shadow outside the seal.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art; chunky deliberate pixels; strict limited 16-color palette; hard dark outline; sparse ordered dithering; legible at 32–48 pixels; harmonize with the supplied world map’s white/cyan/navy palette.
> Output: square 1:1 PNG with genuine alpha transparency outside the irregular stamp silhouette (not a checkerboard pattern).
> Constraints: no words, no letters, no numbers, no flag, no logo, no watermark, no photorealism, no smooth vector gradients, no antialiasing.

### `stamp_asia.png`

> Asia stamp: golden yellow and pale cream seal; central emblem combines a simplified rising sun and mountain gate; a route curves eastward over a broad simplified land silhouette. Create one project-bound game UI asset for a 1990 DOS detective adventure.
>
> Asset family: one of five crime-family route-piece stamps, used as a small earned seal on the painted Passport/world-route dossier after a regional mastermind capture.
> Shared composition: a compact roughly circular case-file seal, jagged perforated pixel edge, bold central regional emblem, one curving dotted travel route ending in a location pin, thick dark outline, no rectangular background, no shadow outside the seal.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art; chunky deliberate pixels; strict limited 16-color palette; hard dark outline; sparse ordered dithering; legible at 32–48 pixels; harmonize with the supplied world map’s white/cyan/navy palette.
> Output: square 1:1 PNG with genuine alpha transparency outside the irregular stamp silhouette (not a checkerboard pattern).
> Constraints: no words, no letters, no numbers, no flag, no logo, no watermark, no photorealism, no smooth vector gradients, no antialiasing.

### `stamp_africa.png`

> Create one project-bound game UI asset for a 1990 DOS detective adventure.
>
> Asset: Africa crime-family route stamp, replacement pass.
> Use: a small earned seal on the painted Passport/world-route dossier after the Africa mastermind capture.
> Subject: one extremely simple green circular pixel seal: unmistakable simplified silhouette of Africa, a small flat-topped acacia tree cutout near the center, a sun disk above it, and a short three-dot route ending in a location pin. No other landmasses, no decorative scenery.
> Composition: centered square 1:1; compact stamp occupies 88% of canvas; jagged perforated pixel edge; thick navy outline; large simple shapes only, designed to remain recognizable at 40 pixels.
> Style: authentic hand-authored 1990 VGA/EGA pixel icon, chunky hard-edged pixels, strict green/cream/navy/cyan palette, no fine texture, no gradients, no antialiasing.
> Transparency: genuine alpha transparency everywhere outside the irregular seal. The outside must contain no color at all. Do not draw or simulate a checkerboard. Do not use white, gray, black, or any solid rectangular canvas behind the seal.
> Constraints: no words, letters, numbers, flag, logo, watermark, photorealism, or shadows outside the seal. Deliver a square PNG with real transparency.

### `stamp_oceania_frontiers.png`

> Oceania and Frontiers stamp: magenta and pale cream seal; central emblem combines a simplified Southern Cross star cluster and ship anchor; a route hops across three small island dots toward an icy frontier peak. Create one project-bound game UI asset for a 1990 DOS detective adventure.
>
> Asset family: one of five crime-family route-piece stamps, used as a small earned seal on the painted Passport/world-route dossier after a regional mastermind capture.
> Shared composition: a compact roughly circular case-file seal, jagged perforated pixel edge, bold central regional emblem, one curving dotted travel route ending in a location pin, thick dark outline, no rectangular background, no shadow outside the seal.
> Style: crisp hand-authored-looking 1990 VGA/EGA pixel art; chunky deliberate pixels; strict limited 16-color palette; hard dark outline; sparse ordered dithering; legible at 32–48 pixels; harmonize with the supplied world map’s white/cyan/navy palette.
> Output: square 1:1 PNG with genuine alpha transparency outside the irregular stamp silhouette (not a checkerboard pattern).
> Constraints: no words, no letters, no numbers, no flag, no logo, no watermark, no photorealism, no smooth vector gradients, no antialiasing.
