# Carmen Sandiego Remake — Project Documentation

This folder is the **start-here context dump** for continuing the project on a new
machine. Read this file top to bottom, then dive into the numbered guides.

> These docs were written on macOS (Apple Silicon). Paths, the emulator, and
> DOSBox are all host-specific — every machine-specific value is flagged
> **`ADAPT`** so you know what to change on the new computer.

---

## 1. What this project is

Two goals, in order:

1. **Corpus extraction** — pull the authentic text + art out of the original DOS
   game *Where in the World is Carmen Sandiego? (Enhanced)*.
2. **Faithful Android remake** — a Jetpack Compose app (`com.acme.carmen`) that
   uses the **real game assets** (nostalgia goal). Runs landscape, renders inside
   a 320×200 virtual canvas letterboxed to the device.

**Status (2026-07-22):** the app builds and runs; **all 30 cities now show the
real in-game photo + authentic in-game description**. The only major thing *not*
done is decoding the `CITIES.DAT` image format from scratch — we **routed around
it** by screen-capturing the real game in DOSBox (see below). Remaining backlog:
sound, sprite animations, the dynamic world-map route line.

---

## 2. The edition (non-obvious, pinned facts)

- Source: archive.org item **`msdos_Where_in_the_World_is_Carmen_Sandiego_Enhanced_1989`**.
- It is actually **"MS-DOS Version 2.1", © 1990 Bröderbund** (the "1989" in the
  item name is wrong). Game files are dated **24 Dec 1996** (a later repackage).
- **`CARMEN.EXE` is PKLITE-compressed.** Decompress before you can read strings:
  `deark -m pklite CARMEN.EXE` → `work/exe_decompressed/CARMEN.000.exe`.
- **The entire prose corpus lives in the EXE**, *not* the `.DAT` files.
- `CITIES.DAT` = 30 per-city records (name + 16-colour palette + compressed image).
  Only the name is plaintext. **Images are NOT decodable yet** — see guide 04.
- `CARMEN.DAT` = graphics. `ACME.DAT` = player save roster (NOT canonical text).
- `DIGISND.DAT` / `MIDISND.DAT` = sound (not wired into the app yet).
- 30 cities, 10 suspects (8 dossier fields each).
- Preserved original typos/spellings to keep byte-accuracy: `tennis raquet`,
  `Ukranian`, toponym `Peking`, and 1990 geography (Soviet Union, Czechoslovakia,
  Yugoslavia).

The `CARMEN01.BMP … CARMEN21.BMP` files in `work/extracted/wwcse/` are **NOT** a
full asset set — they are **screen-captures from one tutorial playthrough** and
only cover 9 cities. The real per-city art for all 30 is in `CITIES.DAT`.

---

## 3. Repository layout

```
whereintheworld/
├── acquisitions/          # read-only original zip (sha256 recorded)
├── work/
│   ├── extracted/wwcse/   # the original game files (CARMEN.EXE, CITIES.DAT, *.DAT, *.BMP)
│   ├── exe_decompressed/  # CARMEN.000.exe  (PKLITE-decompressed EXE — RE target)
│   ├── assets_png/screens/# carmen01–21.png (the 21 BMP playthrough screens as PNG)
│   └── city_captures/     # ★ 21 clean 640×400 in-game city screenshots (this project's output)
├── corpus/                # carmen_corpus.json (byte-exact), game_data.json, carmen_corpus.md
├── scripts/               # extract_corpus.py, build_report.py, gen_kotlin_data.py
├── tools/                 # ★ RE + DOSBox-capture tooling (see guide 02 & 04)
├── reference_screens/     # 21 CARMEN##.BMP + abandonwaredos web_0N shots + INDEX.md
├── android/               # Jetpack Compose app (Kotlin), package com.acme.carmen
└── docs/                  # ← you are here
```

★ = created/populated by the DOSBox-capture work; safe to keep in version control.

**Not a git repo yet.** If you want history, `git init` before copying to the new
machine, or just copy the whole folder. The `android/build/` output and the
ephemeral `/private/tmp/.../scratchpad` are NOT part of the repo.

---

## 4. Guides in this folder

| File | What it covers |
|------|----------------|
| `01-building-and-running.md` | JDK 17 build, installing on the emulator, the **software-GPU ANR trap** and the **headless workaround**, driving the app via `adb`. |
| `02-dosbox-capture-pipeline.md` | How we got real city art: driving the actual DOS game in DOSBox-X and screenshotting all 30 cities. The full automation recipe. |
| `03-city-art-pipeline.md` | Turning a 640×400 capture into a shipping `city_*.png` asset and wiring it into `CityMeta.kt`. |
| `04-reverse-engineering.md` | Everything learned decompiling `CARMEN.EXE` and the `CITIES.DAT` image codec — the format, the cracked LZSS decompressor, and the wall we hit (and why DOSBox capture won instead). |

---

## 5. Prerequisites to install on the new machine

| Tool | Why | Install (macOS) |
|------|-----|-----------------|
| **JDK 17** (exactly) | build the app — 21/25 fail | JetBrains Runtime `jbr-17`, or Temurin 17 |
| **Android SDK** + an AVD | run the app | Android Studio |
| **DOSBox-X** | capture city art from the game | `brew install --cask dosbox-x` |
| **cliclick** | send mouse events to DOSBox | `brew install cliclick` |
| **Python 3 + Pillow + capstone** | crop assets / disassemble | `pip install pillow capstone` |
| **deark** | PKLITE-decompress + extract game files | `brew install deark` |

**macOS permissions that matter** (System Settings → Privacy & Security):
- **Accessibility** must be granted to the terminal/automation process → lets
  `osascript`/`cliclick` send keystrokes+clicks to DOSBox. (It was granted here.)
- **Screen Recording** was **denied** here, so `screencapture` did not work — we
  captured through DOSBox's own menu instead (guide 02). If you grant Screen
  Recording, `screencap`/`screencapture` become available and simplify things.
- **Files and Folders / Full Disk Access**: on 2026-07-22 the terminal lost
  read access to `~/Documents` (dir listing + reading pre-existing files failed
  with "Operation not permitted", though the Read/Write tooling still worked).
  If `ls`/`cat` on the repo fails on the new machine, grant the terminal
  **Full Disk Access**.

---

## 6. The one big lesson

We spent multiple sessions trying to **decode `CITIES.DAT`'s image format** and
hit a hard wall (guide 04). The decompressor is provably correct and the palettes
are correct, but no pixel layout produces a coherent image. **What actually
shipped all 30 photos was giving up on the codec and screen-capturing the real
game in DOSBox** (guide 02) — the game's own decoder does the work, and a raw
screenshot *is* the decoded photo. If you pick this up again: prefer capture over
codec unless you specifically want the RE trophy.
