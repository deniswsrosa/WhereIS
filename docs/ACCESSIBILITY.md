# Accessibility & platform notes

What the app does for accessibility and platform compliance, and the deliberate trade-offs made
against the fixed 320×200 pixel-art canvas.

## Done

- **Target SDK 36 + edge-to-edge.** Builds against `compileSdk`/`targetSdk = 36` (AGP 8.6.0).
  `MainActivity` draws edge-to-edge (`setDecorFitsSystemWindows(false)`) and pads content with the
  system-bar insets, so the HUD is never hidden behind the status/navigation bars or a cutout.
- **Reduced motion.** `isReducedMotion(context)` reads `ANIMATOR_DURATION_SCALE`. When the user has
  turned animations off system-wide, the typewriter report, the dossier type-on, and the jail-cell
  blink snap straight to their end state instead of animating.
- **Captions for audio cues.** `Options ▸ Captions` shows a short on-screen caption for each sound
  cue ("Warrant issued", "The thief got away", …) for deaf / hard-of-hearing or muted-context play.
  Off by default; saved with the career.
- **Screen-reader labels.** Interactive controls carry a `contentDescription` for TalkBack — menu
  items, the Yes/No and dialog buttons, the game toolbar, investigation venues, crime-computer
  attributes, and the "Choose a game" picker rows (continue / delete / new game).
- **48 dp touch targets** on the off-canvas UI — dialog buttons (`DosButton`) and the picker rows
  use `minimumInteractiveComponentSize()` / a 48 dp minimum height without changing their look.
- **Mobile game controls.** Menu and destination rows have explicit large targets; the four main
  toolbar zones are already large on a handset. The crime computer keeps its authentic compact
  CRT rows but duplicates every operation on large keyboard-style previous/next/change/compute
  keys. Venue illustrations provide large alternatives to their compact text rows.
- **Non-colour state.** The selected toolbar tool uses both its green border and a white check mark.
- **Font scaling.** New/off-canvas Android UI uses `sp` and follows the full system font scale.
  Reflowable reading copy inside the 320×200 canvas (city facts, witness clues, dialogs, and the
  World Database) follows the system setting up to 110%. Layout-critical clock/menu/control labels
  remain on the pixel grid so they cannot overlap. TalkBack labels remain available at every scale,
  and Android magnification can enlarge the complete pixel canvas beyond that tested cap.
- **Text contrast.** Recurring foreground/background pairs were audited against WCAG AA. Danger
  copy on black now uses light red (6.68:1), and green action buttons use black text (6.75:1).
  Automated checks protect the core VGA text pairs from regressing below 4.5:1.
- **16 KB page size.** Current AndroidX Compose contributes the small
  `libandroidx.graphics.path.so`; all four bundled ABIs were checked with `readelf` and every LOAD
  segment is aligned to `0x4000` (16 KB). Re-audit whenever a dependency changes native binaries.
- **Adaptive layout.** The whole game renders through a `Virtual` canvas that scales the fixed
  320×200 play-field to fit, letter-/pillar-boxing on tall phones, tablets, and foldables rather
  than stretching or cropping. `configChanges` covers orientation/size/fold/density/fontScale so the
  Activity is not recreated on those changes.

## Deliberate trade-offs

- **In-canvas legacy rows.** Some text rows baked into the 320×200 composition remain below 48 dp
  because expanding them would overlap adjacent DOS controls. They have spoken labels and a large
  equivalent control (crime-computer keys or venue illustration); primary actions and off-canvas
  options meet the mobile target.
- **Large text inside the legacy canvas.** Scaling beyond 110% would overlap adjacent DOS controls.
  The complete canvas remains compatible with Android magnification, while spoken labels provide
  an equivalent path for controls whose visual type cannot grow further.
