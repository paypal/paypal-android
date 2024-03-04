package com.paypal.android.cardpayments

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 *
 * @property approveOrderListener listener to receive callbacks from [CardClient.approveOrder].
 * @property cardVaultListener listener to receive callbacks form [CardClient.vault].
 */
class CardClient internal constructor(
    activity: FragmentActivity,
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val authChallengeLauncher: CardAuthLauncher,
    private val dispatcher: CoroutineDispatcher
) {

    var approveOrderListener: ApproveOrderListener? = null

    /**
     * @suppress
     */
    var cardVaultListener: CardVaultListener? = null

    private var approveOrderId: String? = null
    private val lifeCycleObserver = CardLifeCycleObserver(this)

    private val approveOrderExceptionHandler = CoreCoroutineExceptionHandler { error ->
        notifyApproveOrderFailure(error, approveOrderId)
    }

    private val vaultExceptionHandler = CoreCoroutineExceptionHandler { error ->
        cardVaultListener?.onVaultFailure(error)
    }

    /**
     *  CardClient constructor
     *
     *  @param [activity] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     */
    constructor(activity: FragmentActivity, configuration: CoreConfig) :
            this(
                activity,
                CheckoutOrdersAPI(configuration),
                DataVaultPaymentMethodTokensAPI(configuration),
                AnalyticsService(activity.applicationContext, configuration),
                BrowserSwitchClient(),
                CardAuthLauncher(),
                Dispatchers.Main
            )

    init {
        activity.lifecycle.addObserver(lifeCycleObserver)
    }

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()
    /**
     * Confirm [Card] payment source for an order.
     *
     * @param activity [FragmentActivity] activity used to start 3DS flow (if requested)
     * @param cardRequest [CardRequest] for requesting an order approval
     */
    fun approveOrder(activity: FragmentActivity, cardRequest: CardRequest) {
        // TODO: deprecate this method and offer auth challenge integration pattern (similar to vault)
        approveOrderId = cardRequest.orderId
        analyticsService.sendAnalyticsEvent("card-payments:3ds:started", cardRequest.orderId)

        CoroutineScope(dispatcher).launch(approveOrderExceptionHandler) {
            confirmPaymentSource(activity, cardRequest)
        }
    }

    private suspend fun confirmPaymentSource(activity: FragmentActivity, cardRequest: CardRequest) {
        try {
            val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:confirm-payment-source:succeeded",
                cardRequest.orderId
            )

            if (response.payerActionHref == null) {
                val result = CardResult(response.orderId)
                notifyApproveOrderSuccess(result)
            } else {
                analyticsService.sendAnalyticsEvent(
                    "card-payments:3ds:confirm-payment-source:challenge-required",
                    cardRequest.orderId
                )
                approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()

                // launch the 3DS flow
                val urlScheme = cardRequest.run { Uri.parse(returnUrl).scheme }
                val approveOrderMetadata =
                    ApproveOrderMetadata(cardRequest.orderId, response.paymentSource)
                val options = BrowserSwitchOptions()
                    .url(Uri.parse(response.payerActionHref))
                    .returnUrlScheme(urlScheme)
                    .metadata(approveOrderMetadata.toJSON())

                browserSwitchClient.start(activity, options)
            }
        } catch (error: PayPalSDKError) {
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:confirm-payment-source:failed",
                cardRequest.orderId
            )
            throw error
        }
    }

    /**
     * @suppress
     *
     * Call this method to attach a payment source to a setup token.
     *
     * @param context [Context] Android context reference
     * @param cardVaultRequest [CardVaultRequest] request containing details about the setup token
     * and card to use for vaulting.
     */
    fun vault(context: Context, cardVaultRequest: CardVaultRequest) {
        val applicationContext = context.applicationContext
        CoroutineScope(dispatcher).launch(vaultExceptionHandler) {
            updateSetupToken(applicationContext, cardVaultRequest)
        }
    }

    private suspend fun updateSetupToken(
        context: Context,
        cardVaultRequest: CardVaultRequest
    ) {
        val updateSetupTokenResult = cardVaultRequest.run {
            paymentMethodTokensAPI.updateSetupToken(context, setupTokenId, card)
        }
        val authChallenge = updateSetupTokenResult.approveHref?.let { approveHref ->
            val url = Uri.parse(approveHref)
            CardAuthChallenge(url = url, request = cardVaultRequest)
        }
        val result =
            updateSetupTokenResult.run { CardVaultResult(setupTokenId, status, authChallenge) }
        cardVaultListener?.onVaultSuccess(result)
    }

    fun presentAuthChallenge(activity: FragmentActivity, authChallenge: CardAuthChallenge) {
        authChallengeLauncher.presentAuthChallenge(activity, authChallenge)?.let { launchError ->
            // TODO: notify auth challenger presentation failure
        }
    }

    internal fun handleBrowserSwitchResult(activity: FragmentActivity) {
        authChallengeLauncher.deliverBrowserSwitchResult(activity)?.let { status ->
            when (status) {
                is CardStatus.VaultSuccess -> notifyVaultSuccess(status.result)
                is CardStatus.VaultError -> notifyVaultFailure(status.error)
                CardStatus.VaultCanceled -> notifyVaultCancelation()
            }
        }

        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        if (browserSwitchResult != null && approveOrderListener != null) {
            approveOrderListener?.onApproveOrderThreeDSecureDidFinish()
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> handleBrowserSwitchSuccess(browserSwitchResult)
                BrowserSwitchStatus.CANCELED -> notifyApproveOrderCanceled()
            }
        }
    }

    private fun handleBrowserSwitchSuccess(browserSwitchResult: BrowserSwitchResult) {
        ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)?.let { metadata ->
            try {
                val deepLinkUrl = browserSwitchResult.deepLinkUrl
                val result = parseApproveOrderDeepLink(metadata.orderId, deepLinkUrl)
                notifyApproveOrderSuccess(result)
            } catch (error: PayPalSDKError) {
                analyticsService.sendAnalyticsEvent(
                    "card-payments:3ds:get-order-info:failed",
                    metadata.orderId
                )
                notifyApproveOrderFailure(error, metadata.orderId)
            }
        }
    }

    @Throws(PayPalSDKError::class)
    private fun parseApproveOrderDeepLink(orderId: String, deepLinkUrl: Uri?): CardResult {
        if (deepLinkUrl == null || deepLinkUrl.getQueryParameter("error") != null) {
            throw CardError.threeDSVerificationError
        }

        val state = deepLinkUrl.getQueryParameter("state")
        val code = deepLinkUrl.getQueryParameter("code")
        if (state == null || code == null) {
            throw CardError.malformedDeepLinkError
        }

        val liabilityShift = deepLinkUrl.getQueryParameter("liability_shift")
        return CardResult(orderId, deepLinkUrl, liabilityShift)
    }

    private fun notifyApproveOrderCanceled(browserSwitchResult: BrowserSwitchResult) {
        val metadata = ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)
        val orderId = metadata?.orderId
        analyticsService.sendAnalyticsEvent("card-payments:3ds:challenge:user-canceled", orderId)
        approveOrderListener?.onApproveOrderCanceled()
    }

    private fun notifyApproveOrderSuccess(result: CardResult) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", result.orderId)
        approveOrderListener?.onApproveOrderSuccess(result)
    }

    private fun notifyApproveOrderFailure(error: PayPalSDKError, orderId: String?) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
        approveOrderListener?.onApproveOrderFailure(error)
    }

    private fun notifyVaultSuccess(result: CardVaultResult) {
        analyticsService.sendAnalyticsEvent("card:browser-login:canceled", result.setupTokenId)
        cardVaultListener?.onVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("card:failed", null)
        cardVaultListener?.onVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", null)
        // TODO: consider either adding a listener method or next major version returning a result type
        cardVaultListener?.onVaultFailure(PayPalSDKError(1, "User Canceled"))
    }
}
