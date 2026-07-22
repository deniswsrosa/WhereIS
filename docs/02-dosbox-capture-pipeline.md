# 02 — The DOSBox capture pipeline (how we got real city art)

We could not decode `CITIES.DAT`'s image format from scratch (guide 04). Instead
we let **the game's own decoder** do the work: run the real DOS game in DOSBox-X,
navigate to each city's briefing screen, and take a raw screenshot. A raw
screenshot *is* the decoded photo, at the game's native resolution.

This guide is the full automation recipe. All the tooling lives in `tools/`.

> Values flagged **`ADAPT`** are machine-specific.

---

## 1. Prerequisites

| Tool | Install (macOS) | Used for |
|------|-----------------|----------|
| DOSBox-X | `brew install --cask dosbox-x` | runs the DOS game |
| cliclick | `brew install cliclick` | sends mouse clicks to DOSBox |
| `osascript` | built in | sends keystrokes / clicks menu items |

**macOS permissions (System Settings → Privacy & Security):**
- **Accessibility** → grant to the terminal / automation process. Required for
  `osascript` and `cliclick` to send input to DOSBox. (It was granted here.)
- **Screen Recording** was **denied** on this machine, so `screencapture` did not
  work. We captured through **DOSBox-X's own "Take raw screenshot" menu item**
  instead (see `shot()` below). If you grant Screen Recording, `screencapture`
  becomes available and simplifies capture.

---

## 2. DOSBox config — `tools/carmen.conf`

Key settings (paths are **`ADAPT`** — they point at a session scratchpad):

```ini
[dosbox]
machine=vgaonly
captures=…/scratchpad/caps          # where "Take raw screenshot" writes PNGs
[cpu]
core=normal
cputype=386
cycles=fixed 5000
[render]
scaler=none                          # no upscaling — raw VGA pixels
[autoexec]
mount c …/scratchpad/game            # the extracted game files live here
c:
LOADFIX -32                          # frees low memory; game needs it
CARMEN.EXE
```

- The game files under the mounted `c:` are the ones from
  `work/extracted/wwcse/` (the original `CARMEN.EXE` + `.DAT` set). Copy them into
  the mounted `game/` dir; **do not** use the PKLITE-decompressed EXE here — the
  original runs, the decompressed copy is only for RE.
- `scaler=none` + `machine=vgaonly` gives clean 640×400 (doubled 320×200) frames.

Launch:

```bash
dosbox-x -conf tools/carmen.conf     # ADAPT paths inside the conf first
```

---

## 3. The driver — `tools/drive.sh`

A set of shell helpers that focus the DOSBox window and send input to it. The
important ones:

```bash
focus()          # bring dosbox-x frontmost (via System Events)
sx_text "abc"    # type literal text
sx_key <code>    # press a key by macOS key code
                 #   36=return 49=space 53=esc 123=left 124=right 125=down 126=up
sx_enter / sx_space / sx_esc
park()           # cliclick m:1150,480 — park the mouse over the description
                 # panel, OFF the photo, so the cursor sprite isn't in the crop
shot()           # click Capture ▸ "Take raw screenshot", return newest caps/*.png
```

Usage is `tools/drive.sh <fn> [args]`, e.g.:

```bash
tools/drive.sh focus
tools/drive.sh sx_key 124      # arrow right
tools/drive.sh shot            # prints path of the PNG it just captured
```

**`park()` matters:** the DOS mouse cursor is drawn into the framebuffer, so if
it's over the photo it lands in your screenshot. Park it on the text panel
(coords are **`ADAPT`** to your window position) before every `shot()`.

---

## 4. The capture loop (per city)

For each of the 30 cities, the game shows a briefing screen with the photo + the
description text. The recipe:

1. `focus` the DOSBox window.
2. Navigate the in-game menus to the target city's briefing screen (arrow keys /
   enter — the exact path depends on where you are in the game flow).
3. `park` the mouse off the photo.
4. `shot` → grab the newest PNG from `caps/`.
5. Rename it to the city name (matching `work/city_captures/<City>.png`).

The raw capture is **640×400**. Guide 03 crops it down to the shipping
`city_<name>.png` asset.

Output of this stage lives in **`work/city_captures/`** — currently 21 clean
640×400 in-game screenshots (Athens, Baghdad, Bamako, … Tokyo). These are this
project's own output and safe to keep in version control.

> The 21 vs 30 gap: the remaining cities either weren't reachable in the captured
> playthrough or are pending a re-run. When you re-run, cover the full 30-city
> list from `corpus/game_data.json`.

---

## 5. Why this beat decoding the format

- The game already contains a correct decoder; a screenshot reuses it for free.
- Palettes and pixel layout are guaranteed right because *the game drew it*.
- No RE risk: guide 04 shows the codec RE stalled despite a provably-correct
  decompressor.

Trade-off: you get a rasterized 640×400 photo, not the original indexed-color
source, and capture is semi-manual. For this project (nostalgia fidelity) that's
exactly what we want.
