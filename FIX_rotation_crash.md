# Rotation crash — diagnosis and fix

## Symptom
Rotating the emulator/device crashes the app (and, under the software GPU emulator, can take the
emulator down with it).

## Root cause
`VirtualScreen` (the 320×200 letterbox canvas) computes its scale by dividing by the available
height and then sizes a Box by the result:

```
val fitW = if (maxWidth.value / maxHeight.value > ratio) maxHeight * ratio else maxWidth
val unit = fitW / 320f
Box(Modifier.size(unit * 320f, unit * 200f)) { ... }
```

During a rotation the layout is momentarily measured with a **0 (or unbounded) height**. That makes
`maxWidth/maxHeight` produce `Infinity`/`NaN`, so `unit` becomes `NaN`/`Infinity`, and
`Modifier.size(NaN, NaN)` throws inside Compose layout → the activity crashes.

## Fix 1 — guard VirtualScreen (file: app/src/main/java/com/acme/carmen/ui/CommonUi.kt)
Replace the body of `VirtualScreen` with the version in `CommonUi.kt.fixed` (same folder). Key change:

```kotlin
val wv = maxWidth.value
val hv = maxHeight.value
if (!wv.isFinite() || !hv.isFinite() || wv <= 0f || hv <= 0f) return@BoxWithConstraints
val ratio = 320f / 200f
val fitW: Dp = if (wv / hv > ratio) maxHeight * ratio else maxWidth
val unit = fitW / 320f
if (!unit.value.isFinite() || unit.value <= 0f) return@BoxWithConstraints
```

## Fix 2 — broaden configChanges (file: app/src/main/AndroidManifest.xml)
So a rotation never even recreates the Activity, change the `<activity>` line to:

```
android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden|keyboard|navigation|uiMode|density|layoutDirection|fontScale"
```

## Apply
1. Overwrite `CommonUi.kt` with `CommonUi.kt.fixed`.
2. Edit the manifest's `configChanges` as above.
3. Rebuild:
   ```
   cd android
   export JAVA_HOME=/Users/denisrosa/Library/Java/JavaVirtualMachines/jbr-17.0.9/Contents/Home
   export ANDROID_HOME=/Users/denisrosa/Library/Android/sdk
   ./gradlew :app:assembleDebug
   ```

> NOTE: I could not apply this automatically because, mid-session, all pre-existing files in this
> project became read/write/delete-denied at the OS level ("Operation not permitted" / EPERM) while
> new files still work. Once that access is restored, applying the two changes above takes seconds.
