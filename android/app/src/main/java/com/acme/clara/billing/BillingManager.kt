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
 * `GameState.expansionUnlocked`, reconciled through [onOwnershipChanged] and
 * `ClaraViewModel.reconcileExpansionOwnership()`. Google Play remembers ownership server-side, so
 * "Restore purchase" and a silent re-grant on reinstall are both just [queryExistingPurchases]
 * — this wrapper persists no billing data itself; the repository keeps only the last verified
 * entitlement so an existing buyer can continue playing offline.
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
    private var applicationContext: Context? = null
    private var connectionInFlight = false
    private var onOwnershipChanged: ((Boolean) -> Unit)? = null
    private var onFailed: ((String) -> Unit)? = null
    private val productDetailsWaiters = mutableListOf<(ProductDetails?) -> Unit>()
    private var productDetailsQueryInFlight = false

    private val listener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> onFailed?.invoke(result.debugMessage)
        }
    }

    /** Call once per process (e.g. the first composition of ClaraApp()). [onOwnershipChanged]
     *  receives true for a fresh/owned purchase and false only after a successful Play query says
     *  the product is not owned. Connection/query failures emit neither state, preserving offline
     *  access for an already-known buyer. Safe to call again — a live connection is reused. */
    fun connect(
        context: Context,
        onOwnershipChanged: (Boolean) -> Unit,
        onFailed: (String) -> Unit = {},
    ) {
        applicationContext = context.applicationContext
        this.onOwnershipChanged = onOwnershipChanged
        this.onFailed = onFailed
        if (client?.isReady == true) {
            queryExistingPurchases()
            queryProductDetailsWhenReady()
            return
        }
        ensureConnection()
    }

    /** Start (or restart) setup without creating parallel BillingClient connections. A failed
     *  client is discarded so opening the offer again gets a real retry instead of waiting on a
     *  permanently non-ready singleton. */
    private fun ensureConnection() {
        if (client?.isReady == true || connectionInFlight) return
        val context = applicationContext ?: return
        val c = client ?: BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
            .also { client = it }
        connectionInFlight = true
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (client !== c) return
                connectionInFlight = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases()
                    queryProductDetailsWhenReady()
                } else {
                    if (client === c) client = null
                    c.endConnection()
                    completeProductDetails(null)
                    this@BillingManager.onFailed?.invoke(result.debugMessage)
                }
            }
            override fun onBillingServiceDisconnected() {
                if (client === c) connectionInFlight = false
                // The next offer/restore/connect call invokes startConnection again. Automatic
                // reconnection remains enabled for a disconnect racing an in-flight API call.
            }
        })
    }

    /** Re-checks Google Play's own purchase record — this IS "restore purchases". */
    fun queryExistingPurchases() {
        val c = client?.takeIf { it.isReady } ?: run {
            ensureConnection()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        c.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onFailed?.invoke(result.debugMessage)
                return@queryPurchasesAsync
            }
            val owned = purchases.firstOrNull(::isOwnedPurchase)
            if (owned != null) handlePurchase(owned) else onOwnershipChanged?.invoke(false)
        }
    }

    /** Waits for an in-progress BillingClient connection. [onResult] receives null if setup fails
     *  or Play doesn't recognize [PRODUCT_ID] for this package, track, country, or tester. */
    fun queryProductDetails(onResult: (ProductDetails?) -> Unit) {
        productDetailsWaiters += onResult
        ensureConnection()
        queryProductDetailsWhenReady()
    }

    /** Coalesce every purchase dialog opened during startup into one lookup. In particular, do
     *  not report a false "Not available" merely because Compose rendered before BillingClient's
     *  asynchronous setup callback arrived. */
    private fun queryProductDetailsWhenReady() {
        val c = client?.takeIf { it.isReady } ?: return
        if (productDetailsWaiters.isEmpty() || productDetailsQueryInFlight) return
        productDetailsQueryInFlight = true
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        c.queryProductDetailsAsync(params) { result, queryResult ->
            val details = if (result.responseCode == BillingClient.BillingResponseCode.OK)
                queryResult.productDetailsList.firstOrNull() else null
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onFailed?.invoke(result.debugMessage)
            }
            completeProductDetails(details)
        }
    }

    private fun completeProductDetails(details: ProductDetails?) {
        productDetailsQueryInFlight = false
        val callbacks = productDetailsWaiters.toList()
        productDetailsWaiters.clear()
        callbacks.forEach { it(details) }
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

    private fun isOwnedPurchase(purchase: Purchase): Boolean =
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.contains(PRODUCT_ID)

    private fun handlePurchase(purchase: Purchase) {
        if (!isOwnedPurchase(purchase)) return
        onOwnershipChanged?.invoke(true)
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
