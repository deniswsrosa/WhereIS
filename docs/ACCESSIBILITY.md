# Accessibility & platform notes

What the app does for accessibility and platform compliance, and the deliberate trade-offs made
against the fixed 320×200 pixel-art canvas.

## Done

- **Target SDK 35 + edge-to-edge.** Builds against `compileSdk`/`targetSdk = 35` (AGP 8.6.0).
  `MainActivity` draws edge-to-edge (`setDecorFitsSystemWindows(false)`) and pads content with the
  system-bar insets, so the HUD is never hidden behind the status/navigation bars or a cutout.
- **Reduced motion.** `isReducedMotion(context)` reads `ANIMATOR_DURATION_SCALE`. When the user has
  turned animations off system-wide, the typewriter report, the dossier type-on, and the jail-cell
  blink snap straight to their end state instead of animating.
- **Captions for audio cues.** `Options ▸ Captions` shows a short on-screen caption for each sound
  cue ("Warrant issued", "The thief got away", …) for deaf / hard-of-hearing or muted-context play.
  Off by default; saved with the career.
- **Screen-reader labels.** Interactive controls carry a `contentDescription` for TalkBack — menu
  items, the Yes/No and dialog buttons, the "Choose a game" picker rows (continue / delete /
  new game), the SEE/DEPART/INVESTIGATE/CRIME toolbar (including the locked-by-tour and
  SEE→HIDE states), the DEPART destination list, the INVESTIGATE venue picker, and the CRIME
  computer's SEX/HOBBY/HAIR/FEATURE/VEHICLE/COMPUTE rows.
- **Described, not just present, imagery.** Every `PixelImage` call site now passes an explicit
  `contentDescription`: witness portraits read "Witness: <occupation>", suspect portraits and
  mugshots read "Portrait/Mugshot of <name>", city art reads "Photo of <city>", and the sighting/
  chase story beats (masked face, thug, burglar, cops, escort, jail) are narrated on their
  container so TalkBack gets one description per beat instead of a raw asset slug. Purely
  decorative art (toolbar strip, world-map backdrop, printer/computer chrome, splash screens,
  the DossierCard badge photo) passes `contentDescription = null` so TalkBack skips it instead of
  reading internal asset filenames.
- **48 dp touch targets** on the off-canvas UI — dialog buttons (`DosButton`) and the picker rows
  use `minimumInteractiveComponentSize()` / a 48 dp minimum height without changing their look.
- **16 KB page size.** The app ships no native `.so` libraries (pure Kotlin/Compose), so it is
  compliant with the Nov 2025 16 KB requirement automatically. Re-audit if any native dep is added.
- **Adaptive layout.** The whole game renders through a `Virtual` canvas that scales the fixed
  320×200 play-field to fit, letter-/pillar-boxing on tall phones, tablets, and foldables rather
  than stretching or cropping. `configChanges` covers orientation/size/fold/density/fontScale so the
  Activity is not recreated on those changes.

## Deliberate trade-offs (documented, not yet "done")

- **New TalkBack strings aren't translated yet.** They're routed through `Strings.ui(...)` (the
  same `"ui:<english>"`-keyed catalog every other chrome string uses), so they degrade cleanly to
  English rather than a raw id — but no `ui:` key has been added to the 10 non-English catalogs
  for them yet. A translation pass (same workflow as the rest of `ui.json`/`ui2.json`) is a
  follow-up; it does not block release since the English fallback is always correct.

- **Colour is not the only signal — mostly.** Win vs. loss differ structurally (jail art + report
  text), and cues carry captions. The in-canvas toolbar's *green selection border* is still
  colour-led; a shape/label affordance there is a follow-up.
- **In-canvas tap targets.** Buttons rendered inside the 320×200 canvas (the toolbar, on-screen
  Yes/No) are tied to the canvas scale and can fall below 48 dp on small screens. Enlarging them
  would break the faithful DOS layout, so they carry TalkBack labels but keep their pixel size; the
  off-canvas UI meets 48 dp.
- **Scalable text.** Chrome text on the new off-canvas screens uses `sp`; text baked into the pixel
  canvas does not honour the system font scale by design (it is part of the art grid). A future pass
  could render the almanac / dialog copy in scalable overlays.
- **Contrast.** The VGA palette on its dark grounds generally clears 4.5:1; a per-screen contrast
  audit against WCAG is a follow-up before store submission.
