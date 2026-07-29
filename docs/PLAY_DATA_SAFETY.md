# Google Play — Data Safety form & Families policy

Reference for filling in the Play Console. The app is offline and collects nothing, so most
answers are the simplest ones. Keep this in sync with `PRIVACY.md`.

## Data Safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data encrypted in transit? | N/A — no data leaves the device |
| Do you provide a way for users to request that their data be deleted? | Uninstalling the app deletes all local saves |
| Privacy policy URL | Host `docs/PRIVACY.md` (e.g. GitHub Pages / raw) and link it here |

Notes:
- Local game saves are **not** "collected" under Play's definition (they are neither transmitted
  off-device nor shared). No declaration is required for on-device-only storage.
- The Share action is user-initiated and passes only a spoiler-free text summary to the system
  share sheet; the app transmits nothing itself.
- No third-party SDKs, ads, or analytics — nothing that would force a "data collected" answer.

## Families policy ("Designed for Families")

Because the audience skews to children, the app targets the Families program:

- **Target age group:** all ages (declare in Console).
- **Ads:** none.
- **In-app purchases:** none.
- **Data practices:** no collection/sharing of personal or sensitive data (consistent with the
  Data Safety answers above).
- **Content rating:** complete the IARC questionnaire; the game has mild cartoon "danger" cues
  (a chase / warning), no realistic violence, no objectionable content.
- **SDKs:** keep the dependency set free of ad/analytics SDKs so the app stays Families-eligible.
  (Current deps: Jetpack Compose + Kotlin only.)

## Pre-launch checklist tied to this batch

- [x] Target API level meets Play's requirement (`targetSdk = 35`).
- [x] Edge-to-edge handled via window insets (Android 15 behavior change).
- [x] No native libraries → automatically compliant with the 16 KB page-size requirement.
- [ ] Host the privacy policy at a public URL and paste it into the Console.
- [ ] Complete the Data Safety form and IARC content rating in the Console (manual, one-time).
