# currency catalog — translator brief

Source: `translation/source/currency.json` (flat `{key: english}`, 140 keys). Translate the
**values only**; keys stay byte-identical. Output: `translation/<lang>/currency.json` with the
same keys.

## What this is
This is the World Database (in-game almanac) "Currency:" field, and the same text is reused
verbatim in the venue-3 "counted money called ___" witness clue. Every key is
`currency.<name-without-a-leading-"the">`, e.g. `currency.euro`, `currency.Iraqi dinar`,
`currency.Tajikistani somoni`. There are no placeholders and no ALL-CAPS constraints here —
just 140 real-world currency names.

## Rules
- Translate to the standard name for that currency **in your language**, the way a
  dictionary/almanac would name it (e.g. French "le dinar irakien" → value `dinar irakien`,
  German "Katar-Riyal" → value `Katar-Riyal`).
- Do NOT include a leading article ("the", "el", "der/die/das", "le/la"...) in the value —
  the game already strips a leading "the " and has no other article logic; write the bare
  noun (with its own adjective, if the currency name naturally has one, e.g. "Iraqi dinar").
  If your language's natural phrasing needs an article to sound right in a sentence, that's a
  structural mismatch to flag rather than force — see "reflow" below.
- No length budget — these render as normal prose in the almanac and inside a full clue
  sentence, not a tight UI slot.
- Keep it CONSISTENT with any of the same currency name that might already appear inside this
  language's existing `cities.json` description prose (e.g. if a city description already says
  "the Japanese yen" translated some way, reuse that exact phrasing here for `currency.yen`
  as appropriate to context — check a handful before starting).
- Some currencies repeat almost-identically (e.g. "the peso" vs "the Argentine peso" vs "the
  Chilean peso" are three separate keys) — keep the shared base word consistent across all of
  a family's entries in your language.

## Reflow note (read before translating)
This text is inserted into two different sentence shapes at runtime — always as a direct
object, never as a subject, so a bare (article-less) noun phrase should read naturally in
both:
1. Almanac field: "Currency: <value>"
2. Witness clue: "<lead-in> <pronoun> counted money called <value>."
If your language would naturally want an article or a linking word here that isn't in the
translated value alone, do NOT invent a new placeholder — write the noun form that reads best
bare in both contexts (most languages, this is just the plain currency name), and note any
awkward case in your final report so we can look at it together rather than silently forcing it.
