# 06 — Play Console setup for the World Campaign unlock

_How to turn the already-written billing code into a real, purchasable product. Generated
2026-08-10. The app-side code (`billing/BillingManager.kt`, wired into `ClaraViewModel` and the
Campaign/Passport/Database CTAs) is complete and builds; nothing here requires another code
change except swapping one constant once you've done step 2._

## Why this is needed

`BillingManager.PRODUCT_ID` is currently set to a placeholder, `"world_campaign_unlock"`, that
does not exist in Google Play yet. Until you create a real product with that exact ID (or create
one with a different ID and update the constant), every purchase button in the app will
correctly show **"Not available yet — check back soon."** instead of crashing — that's the
intended fallback, not a bug. Nothing will actually sell until you complete the steps below.

## 1. Prerequisites

- A Play Console account with **Admin** or **Release manager + Financial data** access to this
  app's listing (`com.acme.clara`).
- The app must already exist as a draft or published listing in Play Console — you can't create
  an in-app product before the app itself has been created there.
- At least one signed build uploaded to *any* track (internal testing is enough) — Play Billing
  will not resolve products against a debug build installed via `adb install` or run straight
  from Android Studio. This is the single most common "why is my product not showing up" issue.

## 2. Create the in-app product

1. Play Console → your app → **Monetize** → **Products** → **In-app products**.
2. **Create product**.
3. **Product ID**: enter `world_campaign_unlock` exactly (case-sensitive, no spaces) — this
   matches `BillingManager.PRODUCT_ID` in the code already, so no further code change is needed
   if you use this exact string. If you'd rather use a different ID, that's fine — just update
   the constant in `android/app/src/main/java/com/acme/clara/billing/BillingManager.kt` to match.
4. **Name**: "International Campaign" (or whatever you want players to see in Play's own purchase
   sheet — this is separate from the in-app dialog copy, which the app controls itself).
5. **Description**: a short player-facing line, e.g. "Unlock the worldwide manhunt: 201 new
   destinations, five crime families, and the final showdown with Clara San Diego."
6. **Product type**: this is a one-time purchase, not a subscription — make sure you're in the
   **In-app products** section, not **Subscriptions**.

## 3. Set the price

1. On the product's **Pricing** tab, set a **default price** (e.g. €5.99, matching what the
   in-app dialog copy currently says — if you land on a different number, update the dialog copy
   in `ui/GameMenuBar.kt`'s `PurchaseOfferWindow`, though the price shown to the player is always
   pulled live from Play via `ProductDetails.oneTimePurchaseOfferDetailsList` — the hardcoded
   number in code is only ever a fallback label, never what's actually charged).
2. Either let Play **auto-convert** the price to every other currency (the default, and the
   simplest option), or switch to **manual pricing** per country if you want specific control in
   some markets. Either way, the app needs no further changes — it always displays whatever
   `formattedPrice` Play returns for the buyer's own country/currency.
3. **Activate** the product (top of the page) — a product sitting in Draft/Inactive state will
   never resolve in `queryProductDetailsAsync`, and the app will keep showing "Not available yet."

## 4. Test it before going live

Play Billing will not process real charges against license testers, so this is safe to do freely:

1. Play Console → **Setup** → **License testing** → add the Google account(s) you'll test with.
2. Upload a signed build to the **Internal testing** track and add the same testers to that
   track's tester list.
3. Install the app from the internal-testing opt-in link (not a sideloaded APK — it must come
   through Play for billing to resolve).
4. Open the app, tap **Campaign** in the menu bar (or the Passport/Database CTA) — you should see
   the real price instead of "Not available yet," and tapping **Unlock World Campaign** should
   open Play's native purchase sheet. A license tester's "purchase" is free and instantly
   refunded/reversible from Play Console if you need to test the flow repeatedly.
5. Confirm the purchase actually grants the unlock in-app (Passport/Database open, Campaign menu
   entry disappears) and that force-quitting and reopening the app keeps it unlocked.
6. Confirm **Restore purchase** works: uninstall and reinstall (or sign into a fresh device with
   the same Google account) — the app should silently re-grant the unlock on next launch without
   needing the Restore button at all (that's `BillingManager.queryExistingPurchases()`, called on
   every connect), but the button is there too for anyone who doesn't want to wait.

## 5. Go live

1. Promote the tested build to **Production** (or whichever track you release from).
2. No further Play Console step is needed for the product itself — an Activated in-app product
   is available in every track and country you've configured, automatically.
3. The Play Store listing's `privacy_policy.md` already describes this purchase ("An optional
   in-app purchase unlocks additional game content... handled entirely by Google Play") — no
   changes needed there.

## What's already handled in code (no further setup)

- **Entitlement**: a purchase (or a restore, or a silent re-detected purchase on reconnect) calls
  `ClaraViewModel.unlockExpansion()`, which sets `GameState.expansionUnlocked = true` and is
  persisted in the existing save file. There is no backend and none is needed — Google Play is
  the source of truth for ownership; the app just asks it.
- **Acknowledgement**: every purchase is acknowledged automatically (`BillingManager
  .handlePurchase`) — an unacknowledged purchase auto-refunds after 3 days, so this matters and
  is already done for you.
- **Late purchases**: if a player buys *after* already reaching Case 14 unpaid (where Clara's
  escape earns no promotion — see `data/Masterminds.kt`), `unlockExpansion()` retroactively
  queues the Special Agent promotion so they see it immediately rather than needing to solve one
  more case first.
