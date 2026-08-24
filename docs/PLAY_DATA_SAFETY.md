# Google Play — Data Safety form & Families policy

Reference for filling in the Play Console. Gameplay is offline and the developer collects
nothing. The optional World Campaign purchase uses Google Play Billing. Keep this in sync with
`PRIVACY.md` and re-check the live form wording before submission.

## Data Safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data encrypted in transit? | N/A for developer-collected data; Google Play Billing uses Play's own secure service |
| Do you provide a way for users to request that their data be deleted? | Uninstalling deletes local saves; no save data reaches the developer |
| Privacy policy URL | Host `docs/PRIVACY.md` (e.g. GitHub Pages / raw) and link it here |

Notes:
- Local game saves are **not** "collected" under Play's definition (they are neither transmitted
  off-device nor shared). No declaration is required for on-device-only storage.
- The Share action is user-initiated and passes only a spoiler-free text summary to the system
  share sheet; the app transmits nothing itself.
- Google Play handles payment details directly. The app receives product/ownership results only,
  stores no payment details, and has no developer backend. Google's Data Safety guidance says
  payment data collected directly by a payment service for the transaction need not be declared
  when the app never accesses it: <https://support.google.com/googleplay/android-developer/answer/10787469>.
- No ads, analytics, crash reporting, account system, or developer-operated network service.

## Families policy ("Designed for Families")

Because the audience skews to children, the app targets the Families program:

- **Target age group:** all ages (declare in Console).
- **Ads:** none.
- **In-app purchases:** one clearly described, non-consumable World Campaign unlock, purchased
  through Google Play's native sheet. No loot boxes, virtual currency, subscriptions, or ads.
- **Data practices:** no collection/sharing of personal or sensitive data (consistent with the
  Data Safety answers above).
- **Content rating:** complete the IARC questionnaire; the game has mild cartoon "danger" cues
  (a chase / warning), no realistic violence, no objectionable content.
- **SDKs:** no ad/analytics SDKs. Current notable libraries are Jetpack Compose, WorkManager, and
  Google Play Billing; re-check their Play SDK/Families status in Console at submission time.

## Pre-launch checklist tied to this batch

- [x] Target API level is `targetSdk = 36`.
- [x] Store/privacy copy discloses the optional one-time purchase and Google Play processing.
- [x] Edge-to-edge handled via window insets (Android 15 behavior change).
- [x] No native libraries → automatically compliant with the 16 KB page-size requirement.
- [ ] Host the privacy policy at a public URL and paste it into the Console.
- [ ] Complete the Data Safety form and IARC content rating in the Console (manual, one-time).
