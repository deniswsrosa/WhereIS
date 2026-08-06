# ui2 catalog — translator brief (2026-08 full-coverage pass)

Source: `translation/source/ui2.json` (flat `{key: english}`). Translate the **values only**;
keys stay byte-identical. Output: `translation/<lang>/ui2.json` with exactly the same keys.

## Game context
A faithful remake of a 1990 MS-DOS detective game: you are an Interpol-style detective for the
WDB (World Detective Bureau) chasing Clara San Diego's gang around the world. The register is
retro teletype/case-file English with a light comic touch. Translate **by meaning**, never word
by word — every line must read like a native 1990 localization of the game, matching the tone
already used in this language's `cities.json` / `gameplay.json` catalogs.

## Hard rules
- Placeholders `{0} {1} {2}` and `%s` must survive with the SAME count. `%s` fills in order
  (city names, suspect names, treasures). Reorder them freely to fit your grammar.
- Never translate: suspect names, "Clara San Diego", "Interpol", "WDB", "WhereIS".
- ALL-CAPS values are screen titles / buttons: stay ALL-CAPS (or your script's equivalent).
- `\n` inside a value is a hard line break — keep it, balance the halves.
- The `_F` / `_M` assignment templates describe a female / male thief — write each variant
  grammatically for that gender, don't reuse one for both.
- `{S}`-style pronoun slots do NOT appear here (they live in traits.json) — don't introduce them.

## Length budgets (320×200 pixel UI — hard constraints, pick shorter synonyms when needed)
- menu-bar titles `ui:Game` `ui:Options` `ui:Bureau` `ui:Dossiers`: ≤ 9 chars
- CRT rows `ui:SEX HOBBY HAIR FEATURE VEHICLE`: ≤ 8 chars · `ui:COMPUTE`: ≤ 10
- buttons `ui:Yes No CLOSE SHARE HIDE GOT IT`: ≤ 8 chars
- `tval.*` (values shown on the CRT rows): ≤ 14 chars
- `venue.*`: ≤ 16 chars · `rank.*`: ≤ 18 chars · `ui:PRESS  ANY  KEY  TO  BEGIN`: ≤ 30 chars
- everything else: aim ≤ 1.3× the English length; long prose lines wrap fine.

## Key families
- `ui:<english>` — UI chrome & case-report templates. The text after `ui:` IS the English source.
- `city.<Name>.name` — the place's display name: use your language's standard exonym
  (pt "Moscou", de "Moskau"); copy unchanged when none exists. Consistency matters: the quiz
  answers already localized these (check `quiz.json`).
- `country.<ISO3>` — country names for the passport screen (standard short names).
- `rank.* venue.* occ.* region.name.*` — single labels shown in lists/menus.
- `tval.*` — the crime-computer attribute values (sex / hobby / hair / feature / vehicle).
- `suspect.<i>.<field>` — dossier facts (sex, occupation, hobby, hair, auto, feature1, feature2):
  flavourful case-file prose, keep the humor.
- `noinfo.<Venue>` — a witness at that venue apologising that nobody matching passed through:
  natural spoken language, fits the speaker (croupier, nurse, librarian…).
- `treasure.*` — stolen national treasures, with a natural article ("as Joias da Coroa").
- `nemesis.*` — hushed whispers seeding the finale villain; keep "Clara San Diego" verbatim.
- `achv.<id>.title/.desc` — commendation names (short, punchy) + unlock conditions.
- `day.0..6` — weekday names Monday..Sunday. `ui:a.m.` / `ui:p.m.` — keep "a.m./p.m." or use a
  short local marker; the deadline "Sunday at 5 p.m." should read naturally in your language.
