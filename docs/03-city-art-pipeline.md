# 03 — City-art pipeline (capture → shipping asset → wired into the app)

This turns a raw **640×400** DOSBox capture (guide 02) into a shipping
**`city_<name>.png`** drawable and wires it into `CityMeta.kt`.

---

## 1. What a shipped asset looks like

- Source capture: `work/city_captures/<City>.png` — 640×400, full briefing screen
  (photo + description text + game chrome).
- Shipped asset: `android/app/src/main/res/**drawable-nodpi**/city_<name>.png` —
  just the **photo region**, cropped. Current assets are **144×140** px.
- Placed in **`drawable-nodpi`** so Android does **not** density-scale it — the
  app upscales it itself inside the 320×200 virtual canvas, keeping the chunky
  pixel look. Do not move these into a `drawable-*dpi` bucket.

Naming: lowercase, spaces → underscores. `Mexico City` → `city_mexico_city.png`,
`Rio de Janeiro` → `city_rio_de_janeiro.png`, `New York` → `city_new_york.png`.
This must match the `drawable = "city_…"` value in `CityMeta.kt`.

---

## 2. Cropping the photo out of the capture

The crop is currently an ad-hoc Pillow one-liner (no committed script). The photo
sits in a fixed rectangle of the 640×400 briefing screen; find the box once, then
reuse it for every city since the layout is identical.

```python
from PIL import Image
# ADAPT the box (left, top, right, bottom) to the photo rect in your captures.
BOX = (…, …, …, …)
im = Image.open("work/city_captures/Athens.png").crop(BOX)
# The shipped assets are 144x140; resample to match if your crop differs.
im = im.resize((144, 140), Image.NEAREST)   # NEAREST keeps hard pixel edges
im.save("android/app/src/main/res/drawable-nodpi/city_athens.png")
```

- Use **`Image.NEAREST`** for any resize — bilinear/bicubic blur the retro pixels.
- Because the source is a captured framebuffer, make sure `park()` (guide 02) kept
  the mouse cursor out of the photo box before you crop.

Batch all cities by looping the same `BOX` over `work/city_captures/*.png`,
mapping each filename to its `city_<name>.png` output.

---

## 3. Wiring into `CityMeta.kt`

`android/app/src/main/java/com/acme/carmen/data/CityMeta.kt` holds one
`CityInfo` per city:

```kotlin
data class CityInfo(
    val name: String,
    val region: String,
    val landmark: String,
    val description: String,
    val real: Boolean,          // true = description transcribed verbatim from an in-game screen
    val drawable: String? = null // resource name if we have the authentic photo, e.g. "city_athens"
)
```

To ship a city's photo:
1. Save the cropped PNG as `res/drawable-nodpi/city_<name>.png`.
2. Set that city's `drawable = "city_<name>"` (no extension, no path).
3. The UI resolves the drawable by name at runtime and renders it in the briefing
   panel; a null `drawable` falls back to a placeholder.

`real`/description note: `real = true` means the description text was transcribed
verbatim from an authentic in-game screen. The original Enhanced build renders
these passages **into the artwork**, so they are not extractable strings — they
were transcribed by reading the captured screens. Keep the original 1990 wording
and typos (see the corpus notes) for byte-fidelity to the source.

---

## 4. Verify in the app

```bash
cd android && ./gradlew :app:assembleDebug \
 && adb install -r app/build/outputs/apk/debug/app-debug.apk \
 && adb shell am start -n com.acme.clara/.MainActivity \
 && sleep 3 && adb exec-out screencap -p > /tmp/city.png && open /tmp/city.png
```

Navigate to the city and confirm the photo is sharp (NEAREST-scaled, no blur),
correctly cropped (no chrome, no cursor), and matches the description panel.

---

## 5. Status

- 21 captures in `work/city_captures/`; shipped `city_*.png` drawables cover the
  cities marked with a `drawable` in `CityMeta.kt`.
- To reach all 30: re-run guide 02 for the missing cities, crop with the same
  `BOX`, drop into `drawable-nodpi`, set the `drawable` field.
