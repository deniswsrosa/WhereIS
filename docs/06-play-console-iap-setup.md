# 06 — Play Console setup for the World Campaign unlock

_How to activate and test the app's one-time World Campaign product. Updated 2026-08-24. The
app-side code (`billing/BillingManager.kt`, wired into the Campaign/Passport/Database CTAs) is
complete, uses the permanent product ID below, and ships with `SALES_ENABLED = true`._

## Why this is needed

`BillingManager.PRODUCT_ID` is permanently set to `"world_campaign_unlock"`. Until an active Play
product with that exact ID is available to the installed track/account, every purchase button
safely shows **"Not available yet — check back soon."** Nothing can be charged in that state.

The first internal-track AAB can and should already have `SALES_ENABLED = true`. Google requires a
billing-enabled build on a track before billing product configuration is available. Upload that
signed AAB, create and activate the product, then test the same build from Play; no throwaway
`SALES_ENABLED = false` release or second version is required. See Google's current setup order:
<https://developer.android.com/google/play/billing/getting-ready>.

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
3. **Product ID**: enter `world_campaign_unlock` exactly (case-sensitive, no spaces). Product IDs
   should be treated as permanent, so do not invent a temporary Console ID.
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

Use Play's license-testing account and test payment methods; do not use an ordinary account/card:

1. Play Console → **Setup** → **License testing** → add the Google account(s) you'll test with.
2. Upload a signed build to the **Internal testing** track and add the same testers to that
   track's tester list.
3. Install the app from the internal-testing opt-in link (not a sideloaded APK — it must come
   through Play for billing to resolve).
4. Open the app, tap **Campaign** in the menu bar (or the Passport/Database CTA) — you should see
   the real price instead of "Not available yet," and tapping **Unlock World Campaign** should
   open Play's native purchase sheet. A license tester's "purchase" is free and instantly
   refunded/reversible from Play Console if you need to test the flow repeatedly.
5. Confirm the purchase actually grants the unlock in-app (Wave 1 Passport/Database entries open,
   the Campaign menu reports ownership) and that force-quitting and reopening keeps it unlocked.
6. Confirm **Restore purchase** works: uninstall and reinstall (or sign into a fresh device with
   the same Google account) — the app should silently re-grant the unlock on next launch without
   needing the Restore button at all (that's `BillingManager.queryExistingPurchases()`, called on
   every connect), but the button is there too for anyone who doesn't want to wait.
7. Revoke/refund the test purchase in Play Console, relaunch online, and confirm the campaign
   relocks. Then grant it again, launch once online, switch the device offline, and confirm the
   last verified entitlement remains available while Play cannot be reached.

## 5. Go live

1. Promote the tested build to **Production** (or whichever track you release from).
2. No further Play Console step is needed for the product itself — an Activated in-app product
   is available in every track and country you've configured, automatically.
3. Publish `docs/PRIVACY.md` at a public URL and use it in the listing. It describes the optional
   purchase, Google Play processing, and the app's lack of a developer backend.

## What's already handled in code (no further setup)

- **Entitlement**: a successful Play ownership query grants or revokes the app-wide World Campaign
  marker and updates the active career. Failed/offline queries make no ownership change, so a
  verified buyer can continue playing offline; a successful empty query removes a refunded
  entitlement and stale paid career saves cannot grant it again. There is no developer backend —
  Google Play remains the source of truth for ownership.
- **Acknowledgement**: every purchase is acknowledged automatically (`BillingManager
  .handlePurchase`) — an unacknowledged purchase auto-refunds after 3 days, so this matters and
  is already done for you.
- **Late purchases**: a player who buys after Case 14 keeps the same detective and begins Wave 1.
  Purchase never grants a rank retroactively; the Wave 1 capture earns Special Agent, matching
  the campaign story and the early-purchase path.
