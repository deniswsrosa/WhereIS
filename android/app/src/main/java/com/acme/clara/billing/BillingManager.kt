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
 * [PRODUCT_ID] is the permanent ID that must be created as an active one-time product in Play
 * Console. Until Play recognizes it for the installed track/account, [queryProductDetails]
 * returns null and purchase buttons safely show "not available yet".
 */
object BillingManager {
    const val PRODUCT_ID = "world_campaign_unlock"

    /** Release kill-switch. The billing-capable AAB is validated through Play's internal track,
     *  so purchase entry points are enabled in the artifact that will later be promoted to
     *  production. See docs/06-play-console-iap-setup.md for product activation and testing. */
    const val SALES_ENABLED = true

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
     *  [PRODUCT_ID] for this package, installed track, country, or tester account. */
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
     *  no one-time offer, so incomplete Play configuration can never crash the game. */
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
