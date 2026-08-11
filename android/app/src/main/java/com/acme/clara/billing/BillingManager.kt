package com.acme.clara.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Thin wrapper around Google Play Billing for the single "World Campaign" one-time unlock —
 * the only product this app sells. There is no backend: entitlement is just
 * `GameState.expansionUnlocked`, granted by calling the [onGranted] callback wired to
 * `ClaraViewModel.unlockExpansion()`. Google Play remembers ownership server-side, so
 * "Restore purchase" and a silent re-grant on reinstall are both just [queryExistingPurchases]
 * — nothing is persisted here beyond what the save file already carries.
 *
 * PLACEHOLDER PRODUCT ID. [PRODUCT_ID] does not exist in Play Console yet — see
 * docs/06-play-console-iap-setup.md for the exact steps to create it and swap this constant for
 * the real one. Until then, [queryProductDetails] returns null (Play doesn't recognize an
 * unknown product id) and every purchase button should show "not available yet" rather than
 * attempt a purchase — see the `productDetails == null` branch at each call site.
 */
object BillingManager {
    const val PRODUCT_ID = "world_campaign_unlock"

    /** Kill-switch for this release: the uploaded build didn't request the BILLING permission
     *  (now added — see AndroidManifest.xml), so this build's purchase CTAs stay hidden rather
     *  than show a flow that would fail against Play. Flip to true in the release that follows
     *  the manifest-only rollout, once that build (and the permission it requests) is live. UI
     *  entry points already check this — see the `SALES_ENABLED` reads in GameMenuBar.kt. */
    const val SALES_ENABLED = false

    private var client: BillingClient? = null
    private var onGranted: (() -> Unit)? = null
    private var onFailed: ((String) -> Unit)? = null

    private val listener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> onFailed?.invoke(result.debugMessage)
        }
    }

    /** Call once per process (e.g. the first composition of ClaraApp()). [onGranted] fires for a
     *  fresh purchase, an explicit restore, AND silently on every reconnect if Play already shows
     *  the product owned (covers a reinstall or a new device with no extra "Restore" tap needed).
     *  Safe to call again — a live connection is left alone. */
    fun connect(context: Context, onGranted: () -> Unit, onFailed: (String) -> Unit = {}) {
        this.onGranted = onGranted
        this.onFailed = onFailed
        if (client?.isReady == true) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryExistingPurchases()
            }
            override fun onBillingServiceDisconnected() { /* enableAutoServiceReconnection() retries */ }
        })
    }

    /** Re-checks Google Play's own purchase record — this IS "restore purchases". */
    fun queryExistingPurchases() {
        val c = client?.takeIf { it.isReady } ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        c.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach(::handlePurchase)
        }
    }

    /** [onResult] receives null if the client isn't connected yet OR Play doesn't recognize
     *  [PRODUCT_ID] (the placeholder hasn't been replaced with a real Play Console product). */
    fun queryProductDetails(onResult: (ProductDetails?) -> Unit) {
        val c = client?.takeIf { it.isReady } ?: run { onResult(null); return }
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        c.queryProductDetailsAsync(params) { result, queryResult ->
            val details = if (result.responseCode == BillingClient.BillingResponseCode.OK)
                queryResult.productDetailsList.firstOrNull() else null
            onResult(details)
        }
    }

    /** Launches the native Play purchase sheet. No-op if the client isn't ready or [details] has
     *  no one-time offer (shouldn't happen for a real INAPP product, but the placeholder id
     *  resolves no offers, so this stays a safe no-op rather than a crash). */
    fun launchPurchase(activity: Activity, details: ProductDetails) {
        val c = client?.takeIf { it.isReady } ?: return
        val offerToken = details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken ?: return
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details).setOfferToken(offerToken).build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams)).build()
        c.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.products.contains(PRODUCT_ID)) onGranted?.invoke()
        val c = client ?: return
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            // A failure here self-heals: queryExistingPurchases() re-runs every owned purchase
            // through this same function on the next connect (every app open), so isAcknowledged
            // will still be false and this retries. Logged only so a persistent failure is
            // visible instead of silently retrying forever.
            c.acknowledgePurchase(ackParams) { result ->
                if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    android.util.Log.w("BillingManager", "acknowledgePurchase failed: ${result.debugMessage}")
                }
            }
        }
    }
}
