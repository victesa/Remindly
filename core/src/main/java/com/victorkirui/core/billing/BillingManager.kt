package com.victorkirui.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: com.victorkirui.core.repository.AuthRepository,
    private val purchaseVerifier: PurchaseVerifier,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing setup successful.")
            querySubscriptions()
            queryPurchases()
        } else {
            Log.w(TAG, "Billing setup failed [code ${billingResult.responseCode}]: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. Retrying...")
        startConnection()
    }

    fun querySubscriptions() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_SUBSCRIPTION)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = productDetailsResult.productDetailsList
                Log.d(TAG, "Queried ${list.size} subscription products.")
                _productDetailsList.value = list
            } else {
                Log.w(TAG, "Query product details failed [code ${billingResult.responseCode}]: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasActivePurchases = false
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasActivePurchases = true
                        handlePurchase(purchase)
                    }
                }
                if (!hasActivePurchases) {
                    externalScope.launch {
                        Log.d(TAG, "No active Play Store purchases found on device. Verifying backend profile...")
                        try {
                            val profile = purchaseVerifier.getUserProfile()
                            if (profile != null && profile.success) {
                                val targetTier = if (profile.isProUser) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
                                subscriptionRepository.setSubscriptionTier(targetTier)
                                Log.d(TAG, "Synced tier from backend /v1/me: $targetTier")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to verify backend user profile on empty purchases: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String = BASE_PLAN_MONTHLY
    ): Boolean {
        val allOffers = productDetails.subscriptionOfferDetails ?: emptyList()
        
        allOffers.forEach { offer ->
            Log.d(TAG, "Available Subscription Offer: basePlanId=${offer.basePlanId}, offerId=${offer.offerId}, tags=${offer.offerTags}, token=${offer.offerToken.take(15)}...")
        }

        // 1. Prefer free trial or promotional offer for the requested basePlanId
        var selectedOffer = allOffers.find { offer ->
            offer.basePlanId == basePlanId && offer.pricingPhases.pricingPhaseList.any { phase ->
                phase.priceAmountMicros == 0L
            }
        }

        // 2. Fallback to any offer under basePlanId that has an offerId
        if (selectedOffer == null) {
            selectedOffer = allOffers.find { it.basePlanId == basePlanId && !it.offerId.isNullOrBlank() }
        }

        // 3. Fallback to standard basePlanId
        if (selectedOffer == null) {
            selectedOffer = allOffers.find { it.basePlanId == basePlanId }
        }

        // 4. Fallback to first available offer
        if (selectedOffer == null) {
            selectedOffer = allOffers.firstOrNull()
        }

        val offerToken = selectedOffer?.offerToken ?: ""
        Log.d(TAG, "Selected offerToken for launchBillingFlow: basePlanId=${selectedOffer?.basePlanId}, offerId=${selectedOffer?.offerId}, token=${offerToken.take(15)}...")

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val flowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        val uid = authRepository.currentUser.value?.uid
        if (!uid.isNullOrBlank()) {
            flowParamsBuilder.setObfuscatedAccountId(uid)
        }

        val responseCode = billingClient.launchBillingFlow(activity, flowParamsBuilder.build()).responseCode
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    fun resetBillingState() {
        _billingState.value = BillingState.Idle
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    Log.d(TAG, "onPurchasesUpdated received OK with empty purchases list.")
                    return
                }

                var hasPending = false
                var hasPurchased = false
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                        hasPending = true
                    } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasPurchased = true
                        handlePurchase(purchase)
                    }
                }

                if (hasPending) {
                    _billingState.value = BillingState.PurchasePending
                } else if (hasPurchased) {
                    _billingState.value = BillingState.PurchaseVerifying
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingState.value = BillingState.UserCanceled
            }
            else -> {
                val errorMsg = if (billingResult.debugMessage.isNotBlank()) {
                    billingResult.debugMessage
                } else {
                    "Payment was not completed (${billingResult.responseCode}). Please try again."
                }
                _billingState.value = BillingState.Error(errorMsg)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully on client.")
                    }
                }
            }

            externalScope.launch {
                val productId = purchase.products.firstOrNull() ?: PRODUCT_PRO_SUBSCRIPTION
                Log.d(TAG, "Sending purchase verification to backend: productId=$productId, token=${purchase.purchaseToken}")

                var verified = false
                var attempts = 0
                val maxAttempts = 3
                var delayMs = 1000L

                while (!verified && attempts < maxAttempts) {
                    attempts++
                    try {
                        val response = purchaseVerifier.verifyPurchase(purchase.purchaseToken, productId)
                        if (response.success && response.entitlement != null) {
                            val tierStr = response.entitlement.tier ?: "free"
                            val statusStr = response.entitlement.status?.lowercase() ?: "active"
                            val isPro = (tierStr.equals("pro", ignoreCase = true) || tierStr.equals("premium", ignoreCase = true)) &&
                                    statusStr != "expired" && statusStr != "canceled_expired"
                            val targetTier = if (isPro) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
                            subscriptionRepository.setSubscriptionTier(targetTier)
                            Log.d(TAG, "Backend purchase verification successful (attempt $attempts): tier=$targetTier")
                            verified = true
                        } else {
                            Log.w(TAG, "Backend verification returned failure or no entitlement (attempt $attempts): ${response.message}")
                            if (attempts < maxAttempts) kotlinx.coroutines.delay(delayMs)
                            delayMs *= 2
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Backend verification exception (attempt $attempts): ${e.message}")
                        if (attempts < maxAttempts) kotlinx.coroutines.delay(delayMs)
                        delayMs *= 2
                    }
                }

                // If backend verification timed out or didn't confirm within 3 attempts, trust Google Play PURCHASED state and grant PREMIUM
                if (!verified) {
                    Log.w(TAG, "Backend verification pending/timed out. Trusting Play Store PURCHASED status and granting PREMIUM tier.")
                    subscriptionRepository.setSubscriptionTier(SubscriptionTier.PREMIUM)
                }

                _billingState.value = BillingState.PurchaseSuccess
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_PRO_SUBSCRIPTION = "remindly"
        const val BASE_PLAN_MONTHLY = "monthly-plan"
        const val BASE_PLAN_YEARLY = "yearly-plan"
    }
}

sealed class BillingState {
    object Idle : BillingState()
    object PurchaseVerifying : BillingState()
    object PurchaseSuccess : BillingState()
    object PurchasePending : BillingState()
    object UserCanceled : BillingState()
    data class Error(val message: String) : BillingState()
}
