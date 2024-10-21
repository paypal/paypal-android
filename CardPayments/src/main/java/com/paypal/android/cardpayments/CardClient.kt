package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
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
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val analyticsService: AnalyticsService,
    private val authChallengeLauncher: CardAuthLauncher,
    private val dispatcher: CoroutineDispatcher
) {

    var approveOrderListener: ApproveOrderListener? = null

    // NEXT MAJOR VERSION: rename to vaultListener
    /**
     * @suppress
     */
    var cardVaultListener: CardVaultListener? = null

    private var approveOrderId: String? = null

    private val approveOrderExceptionHandler = CoreCoroutineExceptionHandler { error ->
        notifyApproveOrderFailure(error, approveOrderId)
    }

    private val vaultExceptionHandler = CoreCoroutineExceptionHandler { error ->
        cardVaultListener?.onVaultFailure(error)
    }

    /**
     *  CardClient constructor
     *
     *  @param [context] Android context
     *  @param [configuration] Configuration parameters for client
     */
    constructor(context: Context, configuration: CoreConfig) : this(
        CheckoutOrdersAPI(configuration),
        DataVaultPaymentMethodTokensAPI(configuration),
        AnalyticsService(context.applicationContext, configuration),
        CardAuthLauncher(),
        Dispatchers.Main
    )

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()
    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardRequest] for requesting an order approval
     */
    fun approveOrder(cardRequest: CardRequest) {
        // TODO: deprecate this method and offer auth challenge integration pattern (similar to vault)
        approveOrderId = cardRequest.orderId
        analyticsService.sendAnalyticsEvent("card-payments:3ds:started", cardRequest.orderId)

        CoroutineScope(dispatcher).launch(approveOrderExceptionHandler) {
            // TODO: migrate away from throwing exceptions to result objects
            try {
                val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
                analyticsService.sendAnalyticsEvent(
                    "card-payments:3ds:confirm-payment-source:succeeded",
                    cardRequest.orderId
                )

                if (response.payerActionHref == null) {
                    val result = CardResult(
                        orderId = response.orderId,
                        status = response.status?.name,
                        didAttemptThreeDSecureAuthentication = false
                    )
                    notifyApproveOrderSuccess(result)
                } else {
                    analyticsService.sendAnalyticsEvent(
                        "card-payments:3ds:confirm-payment-source:challenge-required",
                        cardRequest.orderId
                    )
                    approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()

                    val url = Uri.parse(response.payerActionHref)
                    val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
                    approveOrderListener?.onAuthorizationRequired(authChallenge)
                }
            } catch (error: PayPalSDKError) {
                analyticsService.sendAnalyticsEvent(
                    "card-payments:3ds:confirm-payment-source:failed",
                    cardRequest.orderId
                )
                throw error
            }
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
            val updateSetupTokenResult = cardVaultRequest.run {
                paymentMethodTokensAPI.updateSetupToken(applicationContext, setupTokenId, card)
            }
            val authChallenge = updateSetupTokenResult.approveHref?.let { approveHref ->
                val url = Uri.parse(approveHref)
                CardAuthChallenge.Vault(url = url, request = cardVaultRequest)
            }
            val result =
                updateSetupTokenResult.run { CardVaultResult(setupTokenId, status, authChallenge) }
            cardVaultListener?.onVaultSuccess(result)
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: ComponentActivity, authChallenge: CardAuthChallenge) =
        authChallengeLauncher.presentAuthChallenge(activity, authChallenge)

    fun completeAuthChallenge(intent: Intent, authState: String): CardStatus {
        val status = authChallengeLauncher.completeAuthRequest(intent, authState)
        when (status) {
            is CardStatus.VaultSuccess -> notifyVaultSuccess(status.result)
            is CardStatus.VaultError -> notifyVaultFailure(status.error)
            is CardStatus.VaultCanceled -> notifyVaultCancelation()
            is CardStatus.ApproveOrderError ->
                notifyApproveOrderFailure(status.error, status.orderId)

            is CardStatus.ApproveOrderSuccess -> notifyApproveOrderSuccess(status.result)
            is CardStatus.ApproveOrderCanceled -> notifyApproveOrderCanceled(status.orderId)
            CardStatus.NoResult -> {
                // ignore
            }
        }
        return status
    }

    private fun notifyApproveOrderCanceled(orderId: String?) {
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

    /**
     * Call this method at the end of the card flow to clear out all observers and listeners
     */
    fun removeObservers() {
        approveOrderListener = null
        cardVaultListener = null
    }
}
