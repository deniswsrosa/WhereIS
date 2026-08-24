# 01 — Building and running the Android app

The remake is a standard Gradle/Compose project under `android/`. This guide covers
building it, installing on an emulator, the two traps that cost the most time
(the **software-GPU ANR** and the **rotation crash**), and how to drive the app
headlessly over `adb`.

> Values flagged **`ADAPT`** are machine-specific — change them on a new machine.

---

## 1. Toolchain (must match)

| Requirement | Value | Notes |
|-------------|-------|-------|
| JDK | **17, exactly** | 21 and 25 both fail the Compose/Gradle build here. |
| `JAVA_HOME` | `…/Library/Java/JavaVirtualMachines/jbr-17.0.9/Contents/Home` | **`ADAPT`** — any JDK 17 works (Temurin 17, JBR 17). |
| `ANDROID_HOME` | `~/Library/Android/sdk` | **`ADAPT`** |
| Gradle | wrapper (`./gradlew`) | do not use a system gradle |

Set the env for a shell session:

```bash
export JAVA_HOME=/Users/denisrosa/Library/Java/JavaVirtualMachines/jbr-17.0.9/Contents/Home   # ADAPT
export ANDROID_HOME=/Users/denisrosa/Library/Android/sdk                                       # ADAPT
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Verify: `java -version` must print `17.x`.

---

## 2. Build

```bash
cd android
./gradlew :app:assembleDebug
```

Output APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
Package id: **`com.acme.clara`**, launch activity `com.acme.clara.MainActivity`.

A clean build after JDK changes: `./gradlew clean :app:assembleDebug`.

---

## 3. Emulator / device

The app is **landscape-only** and renders into a fixed **320×200 virtual canvas**
letterboxed to the screen (see `ui/CommonUi.kt`, `VirtualScreen`). Any AVD works,
but read §5 before you pick a GPU mode.

Install + launch:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.acme.clara/.MainActivity
# force landscape if the AVD boots portrait:
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
```

---

## 4. The rotation crash (fixed — verify it stays fixed)

Rotating used to crash the activity (and, under the software GPU emulator, could
take the emulator down with it). Full write-up in **`../FIX_rotation_crash.md`**.

Root cause: during a rotation the layout is momentarily measured with 0/unbounded
height, so `VirtualScreen`'s `maxWidth/maxHeight` yields `Infinity`/`NaN`, `unit`
becomes `NaN`, and `Modifier.size(NaN, NaN)` throws inside Compose layout.

The two fixes (both should already be in the tree — confirm before shipping):
1. **Guard `VirtualScreen`** — bail out of `BoxWithConstraints` when width/height
   aren't finite and positive (see the snippet in `FIX_rotation_crash.md`).
2. **Broaden `configChanges`** in `AndroidManifest.xml` so a rotation never
   recreates the activity.

To confirm it's fixed: `adb shell am start …`, then toggle rotation
(`user_rotation 0` ↔ `1`) a few times and watch `adb logcat` for an
`IllegalArgumentException`/`FATAL EXCEPTION` in Compose layout. None = good.

---

## 5. The software-GPU ANR trap (and the headless workaround)

On a headless / software-rendered emulator (`-gpu swiftshader_indirect` or
`-gpu off`), Compose can peg the render thread and the app goes **ANR** ("App
isn't responding") before you ever see a frame — the UI never becomes
interactive, so you can't dismiss it either.

**Workaround that worked here:** don't rely on the GUI. Boot the emulator
**headless** and drive everything through `adb` (install, launch, screenshot,
input), never touching the on-screen window:

```bash
# boot headless (ADAPT the AVD name)
emulator -avd Pixel_6_API_34 -no-window -no-audio -gpu swiftshader_indirect -no-snapshot &
adb wait-for-device
# poll until fully booted
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
```

If you have a hardware-GPU host (real GPU passthrough or a physical device),
prefer that — the ANR does not occur and the app is responsive.

---

## 6. Driving the app over `adb`

```bash
# screenshot the current frame
adb exec-out screencap -p > /tmp/frame.png

# tap / swipe (coords are device pixels, not virtual-canvas pixels)
adb shell input tap 640 360
adb shell input swipe 200 360 900 360 250

# text + keys
adb shell input text "hello"
adb shell input keyevent KEYCODE_ENTER
adb shell input keyevent KEYCODE_BACK

# watch for crashes / ANRs
adb logcat -c && adb logcat '*:E' | grep -i -E 'carmen|AndroidRuntime|ANR'
```

Because the canvas is a letterboxed 320×200, tap targets move when the window
size changes — screenshot first, then compute the tap coordinate from the actual
frame rather than hard-coding.

---

## 7. Quick smoke test

```bash
cd android && ./gradlew :app:assembleDebug \
 && adb install -r app/build/outputs/apk/debug/app-debug.apk \
 && adb shell am start -n com.acme.clara/.MainActivity \
 && sleep 3 && adb exec-out screencap -p > /tmp/carmen_boot.png
open /tmp/carmen_boot.png
```

You should see the title/briefing screen with a real city photo — not a blank
letterbox and no ANR dialog.
