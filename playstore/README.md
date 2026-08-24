# Google Play submission kit

Everything in this folder maps 1:1 onto a Play Console field. Work through it top to bottom.

## ⚠️ Decide BEFORE the first upload (cannot be changed later)

- **Application ID** is currently `com.acme.clara` (`android/app/build.gradle.kts`). It is
  **permanent** once the first build is uploaded. If you'd rather ship under your own domain
  (e.g. `com.deniswsrosa.clara` or `org.wdb.clara`), rename it *now* — it's a one-line change
  plus moving the Kotlin package, and I can do it on request.
- The **app name** shown under the icon comes from `android:label` ("Clara San Diego").
  The store listing title is set separately (see below) and can differ.

## 1. Store listing → Main store listing

For **each language** (add all 11 under "Manage translations"), paste from
`listing/<locale>/`:

| Console field                | File                          | Limit |
|------------------------------|-------------------------------|-------|
| App name                     | `title.txt`                   | 30    |
| Short description            | `short_description.txt`       | 80    |
| Full description             | `full_description.txt`        | 4000  |

Locales prepared: `en-US` (default), `pt-BR`, `es-ES`, `fr-FR`, `de-DE`, `it-IT`,
`nl-NL`, `pl-PL`, `ru-RU`, `tr-TR`, `id`.

## 2. Store listing → Graphics

| Console field    | File                                        | Spec              |
|------------------|---------------------------------------------|-------------------|
| App icon         | `graphics/icon_512.png`                     | 512×512 PNG       |
| Feature graphic  | `graphics/feature_graphic_1024x500_greatest_chase.png`     | 1024×500 PNG      |
| Phone screenshots| `graphics/screenshots/phone/01…08.png`      | 8 framed shots, 1920×1080 |

Store-style marketing slides over the key-art backdrop: slide 1 is the hook (brand,
"BECOME A WORLD DETECTIVE", feature chips, Clara sticker, gameplay proof), then seven
benefit slides with big pixel headlines, a feature chip each, and the real gameplay
screen floating at a slight tilt with depth shadow. The debug menu is scrubbed from
every capture. Upload in filename order. Clean unframed captures live in
`graphics/screenshots/raw/` (e.g. for future localized variants). English slides are
used for every locale (allowed).

## 3. App category & tags

- App or game: **Game**
- Category: **Educational** (alternative: Trivia)
- Tags: Geography, Detective, Retro, Single player, Offline

## 4. Content rating questionnaire (IARC)

Honest answers for this game — expect an **Everyone / PEGI 3** rating:

- Violence: none (a cartoon burglar runs across the screen; arrests happen off-screen — answer
  "no realistic violence"; if asked about "mild cartoon violence", the burglar/jail imagery
  qualifies as mild at most)
- Sexuality / nudity / profanity / drugs / gambling (simulated or real): **No**
  (the game has a Casino *venue* with a Croupier witness in the paid expansion — nobody gambles;
  no gambling mechanics exist)
- User interaction / user-generated content / chat: **No**
- Shares location: **No**
- Digital purchases: **Yes** (the expansion unlock, once wired to Play Billing)

## 5. Data safety form

The app collects **no data**: no accounts, no analytics, no ads SDKs, no network calls at
runtime. Saves and settings are local files. Reminders use local notifications only.

- Does your app collect or share any of the required user data types? → **No**
- Is all of the user data collected by your app encrypted in transit? → N/A (nothing collected)
- Do you provide a way for users to request that their data is deleted? → N/A

## 6. Privacy policy

Play requires a public URL even when nothing is collected. `privacy_policy.md` is ready to
host — push it to a GitHub Pages site (or a gist) and paste the URL into
App content → Privacy policy.

## 7. Building the release bundle (AAB)

The Console needs an **app bundle** signed with an upload key:

```bash
# one-time: create the upload keystore (keep it + the passwords safe!)
keytool -genkeypair -v -keystore ~/keystores/clara-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias clara

# add to android/keystore.properties (git-ignored):
#   storeFile=/home/<you>/keystores/clara-upload.jks
#   storePassword=…
#   keyAlias=clara
#   keyPassword=…
```

Release signing is **already wired**: `signingConfigs.release` reads
`android/keystore.properties` (git-ignored) when it exists. To rebuild the bundle:

```bash
cd android && ./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Version is `versionCode 6` / `versionName "1.3"` — bump `versionCode` on every upload.

## 8. Remaining loose ends before going live

- [ ] Application ID decision (see top)
- [x] Release signing config + keystore (upload key at `~/keystores/clara-upload.jks`, credentials in `android/keystore.properties` — BACK BOTH UP)
- [x] Play Billing integration for the one-time World Campaign unlock; the debug menu is guarded
      by `BuildConfig.DEBUG` and the release audit verifies its labels are absent from release DEX.
- [ ] Host the privacy policy and paste its URL
- [ ] Google Play Console developer account ($25 one-time) with contact email
- [ ] Optional: tablet screenshots (Play accepts the phone set, but 7"/10" shots improve
      tablet placement — capture the same way on a tablet AVD)
