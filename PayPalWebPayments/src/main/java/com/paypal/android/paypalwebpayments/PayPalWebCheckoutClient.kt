package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.ShopperSessionWithAppSwitchEligibility
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
    private val sessionStore: PayPalWebCheckoutSessionStore,
    private val deviceInspector: DeviceInspector,
    private val coreConfig: CoreConfig,
    private val updateClientConfigAPI: UpdateClientConfigAPI,
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility,
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    // Enable app switch by switching this flag to true
    private val appSwitchWhenEligible: Boolean = false

    // for analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null
    private var appSwitchEnabled: Boolean = false

    constructor(
        context: Context,
        configuration: CoreConfig
    ) : this(
        analytics = PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        payPalWebLauncher = PayPalWebLauncher(context),
        sessionStore = PayPalWebCheckoutSessionStore(),
        deviceInspector = DeviceInspector(context),
        coreConfig = configuration,
        patchCCOWithAppSwitchEligibility = PatchCCOWithAppSwitchEligibility(configuration),
        createShopperSessionAPI = CreateShopperSessionWithAppSwitchEligibilityAPI(configuration),
        updateClientConfigAPI = UpdateClientConfigAPI(context, configuration),
    )

    /**
     * Capture instance state for later restoration. This can be useful for recovery during a
     * process kill.
     */
    val instanceState: String
        get() = sessionStore.toBase64EncodedJSON()

    /**
     * Restore a feature client using instance state. @see [instanceState]
     */
    fun restore(instanceState: String) {
        sessionStore.restore(instanceState)
    }

    /**
     * Confirm PayPal payment source for an order.
     *
     * Fires the Shopper Session API call immediately, in parallel with [createOrderHandler].
     * Once the merchant calls back with an order ID, awaits the SSID result and routes to
     * app switch or browser based on eligibility.
     *
     * @param activity The activity to launch the PayPal web checkout from
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param createOrderHandler [CreateOrderHandler] invoked with a completion callback;
     *   must call the callback exactly once with the order ID or an error
     * @param callback [PayPalWebStartCallback] to receive the launch result
     */
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        createOrderHandler: CreateOrderHandler,
        callback: PayPalWebStartCallback
    ) {
        val returnToAppStrategy = ReturnToAppStrategy.AppLink(request.returnToAppUrlConfig.returnAppUrl)
        val buyerEmail = (request.userIdentity as? PayPalUserIdentity.Email)?.email
        val paymentType = if (request.userAction == PayPalUserAction.PAY_NOW) "commit" else "continue"

        // Fire SSID immediately — runs in parallel with createOrderHandler
        val sessionDeferred = applicationScope.async {
            createShopperSessionAPI(
                context = activity,
                bnCode = coreConfig.bnCode,
                flowType = FLOW_TYPE_CHECKOUT,
                paymentType = paymentType,
                tokenType = CreateShopperSessionWithAppSwitchEligibilityAPI.TOKEN_TYPE_ORDER_ID,
                buyerEmail = buyerEmail,
                paypalNativeAppInstalled = deviceInspector.isPayPalInstalled,
                returnAppUrl = request.returnToAppUrlConfig.returnAppUrl,
                cancelAppUrl = request.returnToAppUrlConfig.cancelAppUrl,
                fallbackSchemeUrl = request.returnToAppUrlConfig.fallbackSchemeUrl,
            )
        }

        createOrderHandler.createOrder { orderResult ->
            when (orderResult) {
                is CreateOrderResponse.Success -> {
                    checkoutOrderId = orderResult.orderId
                    applicationScope.launch {
                        val launchResult = when (val sessionResult = sessionDeferred.await()) {
                            is APIResult.Success ->
                                routeCheckoutWithSsid(activity, sessionResult.data, orderResult.orderId, returnToAppStrategy)
                            is APIResult.Failure ->
                                // Silent fallback — merchant is not notified of SSID failure
                                fallbackCheckoutWithPatchCco(activity, orderResult.orderId, returnToAppStrategy)
                        }
                        withContext(Dispatchers.Main) {
                            callback.onPayPalWebStartResult(launchResult)
                        }
                    }
                }
                is CreateOrderResponse.Failure -> {
                    sessionDeferred.cancel()
//                    analytics.notify(CheckoutEvent.FAILED, checkoutOrderId = null, appSwitchEnabled)
                    applicationScope.launch(Dispatchers.Main) {
                        callback.onPayPalWebStartResult(
                            PayPalPresentAuthChallengeResult.Failure(
                                PayPalWebCheckoutError.orderCreationFailed(orderResult.error)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Vault PayPal as a payment method.
     *
     * Fires the Shopper Session API call immediately, in parallel with [createSetupTokenHandler].
     * Once the merchant calls back with a setup token ID, awaits the SSID result and routes to
     * app switch or browser based on eligibility.
     *
     * @param activity the ComponentActivity to launch the auth challenge from
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     * @param createSetupTokenHandler [CreateSetupTokenHandler] invoked with a completion callback;
     *   must call the callback exactly once with the setup token ID or an error
     * @param callback callback to receive the launch result
     */
    fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest,
        createSetupTokenHandler: CreateSetupTokenHandler,
        callback: PayPalWebVaultCallback
    ) {
        val returnToAppStrategy = ReturnToAppStrategy.AppLink(request.returnToAppUrlConfig.returnAppUrl)
        val buyerEmail = (request.userIdentity as? PayPalUserIdentity.Email)?.email
        val paymentType = if (request.userAction == PayPalUserAction.SETUP_NOW) "commit" else "continue"

        // Fire SSID immediately — runs in parallel with createSetupTokenHandler
        val sessionDeferred = applicationScope.async {
            createShopperSessionAPI(
                context = activity,
                bnCode = coreConfig.bnCode,
                flowType = FLOW_TYPE_VAULT,
                paymentType = paymentType,
                tokenType = CreateShopperSessionWithAppSwitchEligibilityAPI.TOKEN_TYPE_VAULT_ID,
                buyerEmail = buyerEmail,
                paypalNativeAppInstalled = deviceInspector.isPayPalInstalled,
                returnAppUrl = request.returnToAppUrlConfig.returnAppUrl,
                cancelAppUrl = request.returnToAppUrlConfig.cancelAppUrl,
                fallbackSchemeUrl = request.returnToAppUrlConfig.fallbackSchemeUrl,
            )
        }

        createSetupTokenHandler.createSetupToken { tokenResult ->
            when (tokenResult) {
                is CreateSetupTokenResponse.Success -> {
                    vaultSetupTokenId = tokenResult.setupTokenId
                    applicationScope.launch {
                        val sessionResult = sessionDeferred.await()
                        val launchResult = when (sessionResult) {
                            is APIResult.Success ->
                                routeVaultWithSsid(activity, sessionResult.data, tokenResult.setupTokenId, returnToAppStrategy)
                            is APIResult.Failure ->
                                // Silent fallback — use standard vault URL
                                fallbackVault(activity, tokenResult.setupTokenId, returnToAppStrategy)
                        }
                        withContext(Dispatchers.Main) {
                            callback.onPayPalWebVaultResult(launchResult)
                        }
                    }
                }
                is CreateSetupTokenResponse.Failure -> {
                    sessionDeferred.cancel()
//                    analytics.notify(VaultEvent.FAILED, vaultSetupTokenId = null, appSwitchEnabled)
                    applicationScope.launch(Dispatchers.Main) {
                        callback.onPayPalWebVaultResult(
                            PayPalPresentAuthChallengeResult.Failure(
                                PayPalWebCheckoutError.setupTokenCreationFailed(tokenResult.error)
                            )
                        )
                    }
                }
            }
        }
    }

    // ── finishStart / finishVault ─────────────────────────────────────────────

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishStart(intent: Intent): PayPalWebCheckoutFinishStartResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    analytics.notify(
                        CheckoutEvent.SUCCEEDED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    analytics.notify(
                        CheckoutEvent.CANCELED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    analytics.notify(
                        CheckoutEvent.FAILED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishVault(intent: Intent): PayPalWebCheckoutFinishVaultResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> {
                    analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishVaultResult.Failure -> {
                    analytics.notify(VaultEvent.FAILED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.Canceled -> {
                    analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            return result
        }

    // ── Routing helpers ───────────────────────────────────────────────────────

    /**
     * Launches the browser switch and persists the auth state so [finishStart] / [finishVault]
     * can retrieve it without requiring the caller to pass it back.
     */
    private fun launch(
        activity: Activity,
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        val result = payPalWebLauncher.launchWithUrl(activity, uri, token, tokenType, returnToAppStrategy)
        if (result is PayPalPresentAuthChallengeResult.Success) {
            sessionStore.authState = result.authState
        }
        return result
    }

    private suspend fun routeCheckoutWithSsid(
        activity: Activity,
        session: ShopperSessionWithAppSwitchEligibility,
        orderId: String,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        val ssid = session.shopperSessionId
        return when {
            session.appSwitchEligible && deviceInspector.isPayPalInstalled && session.redirectURL != null -> {
                appSwitchEnabled = true
                val uri = buildSsidUri(session.redirectURL!!, TOKEN_PARAM_CHECKOUT, orderId, ssid)
                launch(activity, uri, orderId, TokenType.ORDER_ID, returnToAppStrategy)
            }
            session.checkoutFallbackUrl != null -> {
                appSwitchEnabled = false
                val uri = buildSsidUri(session.checkoutFallbackUrl!!, TOKEN_PARAM_CHECKOUT, orderId, ssid)
                launch(activity, uri, orderId, TokenType.ORDER_ID, returnToAppStrategy)
            }
            else ->
                // SSID gave no URLs — silent fallback to patchCCO
                fallbackCheckoutWithPatchCco(activity, orderId, returnToAppStrategy)
        }
    }

    private suspend fun fallbackCheckoutWithPatchCco(
        activity: Activity,
        orderId: String,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        appSwitchEnabled = false
        val returnUrl = returnToAppStrategy.returnUrl
        val fallbackUri = buildPayPalCheckoutUri(orderId, PayPalWebCheckoutFundingSource.PAYPAL, returnUrl)
        val launchUri = getLaunchUri(activity, orderId, TokenType.ORDER_ID, fallbackUri)
        return launch(activity, launchUri, orderId, TokenType.ORDER_ID, returnToAppStrategy)
    }

    private fun routeVaultWithSsid(
        activity: Activity,
        session: ShopperSessionWithAppSwitchEligibility,
        setupTokenId: String,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        val ssid = session.shopperSessionId
        return when {
            session.appSwitchEligible && deviceInspector.isPayPalInstalled && session.redirectURL != null -> {
                appSwitchEnabled = true
                val uri = buildSsidUri(session.redirectURL!!, TOKEN_PARAM_VAULT, setupTokenId, ssid)
                launch(activity, uri, setupTokenId, TokenType.VAULT_ID, returnToAppStrategy)
            }
            session.checkoutFallbackUrl != null -> {
                appSwitchEnabled = false
                val uri = buildSsidUri(session.checkoutFallbackUrl!!, TOKEN_PARAM_VAULT, setupTokenId, ssid)
                launch(activity, uri, setupTokenId, TokenType.VAULT_ID, returnToAppStrategy)
            }
            else -> fallbackVault(activity, setupTokenId, returnToAppStrategy)
        }
    }

    private fun fallbackVault(
        activity: Activity,
        setupTokenId: String,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        appSwitchEnabled = false
        val uri = buildPayPalVaultUri(setupTokenId)
        return launch(activity, uri, setupTokenId, TokenType.VAULT_ID, returnToAppStrategy)
    }

    // ── URI builders ──────────────────────────────────────────────────────────

    /**
     * Appends a token query param and optional SSID to a base checkout/vault URL.
     */
    private fun buildSsidUri(
        baseUrl: String,
        tokenParam: String,
        tokenValue: String,
        ssid: String?,
    ): Uri {
        val builder = baseUrl.toUri().buildUpon()
            .appendQueryParameter(tokenParam, tokenValue)
        ssid?.let { builder.appendQueryParameter(SSID_QUERY_PARAM, it) }
        return builder.build()
    }

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        funding: PayPalWebCheckoutFundingSource,
        returnUrl: String?
    ): Uri {
        return baseUrl.toUri()
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", returnUrl)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", funding.value)
            .appendQueryParameter("integration_artifact", UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT)
            .build()
    }

    private fun buildPayPalVaultUri(
        setupTokenId: String
    ): Uri {
        return baseUrl.toUri()
            .buildUpon()
            .appendPath("agreements")
            .appendPath("approve")
            .appendQueryParameter("approval_session_id", setupTokenId)
            .build()
    }

    private val baseUrl: String
        get() = when (coreConfig.environment) {
            Environment.LIVE -> "https://paypal.com/"
            Environment.SANDBOX -> "https://sandbox.paypal.com/"
        }

    private suspend fun getLaunchUri(
        context: Context,
        token: String,
        tokenType: TokenType,
        fallbackUri: Uri
    ): Uri {
        return if (appSwitchWhenEligible && deviceInspector.isPayPalInstalled) {
            val patchCcoResult = patchCCOWithAppSwitchEligibility(
                context = context,
                orderId = token,
                tokenType = tokenType,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
            when (patchCcoResult) {
                is APIResult.Success -> {
                    appSwitchEnabled = patchCcoResult.data.appSwitchEligible
                    patchCcoResult.data.launchUrl?.toUri() ?: fallbackUri
                }

                is APIResult.Failure -> {
                    appSwitchEnabled = false
                    fallbackUri
                }
            }
        } else {
            appSwitchEnabled = false
            fallbackUri
        }
    }

    companion object {
        private const val FLOW_TYPE_CHECKOUT = "EC_ONE_TIME_CHECKOUT"
        private const val FLOW_TYPE_VAULT = "BILLING_WITHOUT_PURCHASE"

        private const val TOKEN_PARAM_CHECKOUT = "token"
        private const val TOKEN_PARAM_VAULT = "approval_session_id"

        /** Query param name for the Shopper Session ID, appended to check out/vault URLs. */
        private const val SSID_QUERY_PARAM = "shoppersSessionId"
    }
}
