package com.paypal.android.cardpayments

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService

/**
 * Use this client to approve an order with a [Card].
 *
 */
class CardClient internal constructor(
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val analyticsService: AnalyticsService
) {

    private var approveOrderId: String? = null

    /**
     *  CardClient constructor
     *
     *  @param [context] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     */
    constructor(context: Context) :
            this(
                CheckoutOrdersAPI(),
                DataVaultPaymentMethodTokensAPI(),
                AnalyticsService(context.applicationContext)
            )

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()
    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param callback [CardApproveOrderListener] callback used to return a result to the caller
     */
    suspend fun approveOrder(cardRequest: CardRequest): CardApproveOrderResult = try {
        // TODO: migrate away from throwing exceptions to result objects
        approveOrderId = cardRequest.orderId
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:started",
            cardRequest.config,
            cardRequest.orderId,
        )

        val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:succeeded",
            cardRequest.config,
            cardRequest.orderId
        )

        val authChallengeUrl = response.payerActionHref
        if (authChallengeUrl == null) {
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:succeeded",
                cardRequest.config,
                response.orderId
            )
            CardApproveOrderResult.Success(
                orderId = response.orderId,
                status = response.status?.name,
                didAttemptThreeDSecureAuthentication = false
            )
        } else {
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:confirm-payment-source:challenge-required",
                cardRequest.config,
                cardRequest.orderId
            )
            val returnUrlScheme: String? = Uri.parse(cardRequest.returnUrl).scheme
            val authChallenge = CardAuthChallenge.ApproveOrder(
                url = Uri.parse(authChallengeUrl),
                request = cardRequest,
                returnUrlScheme = returnUrlScheme
            )
            CardApproveOrderResult.AuthorizationRequired(authChallenge)
        }
    } catch (error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:failed",
            cardRequest.config,
            cardRequest.orderId
        )
        CardApproveOrderResult.Failure(error)
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
    suspend fun vault(context: Context, cardVaultRequest: CardVaultRequest): CardVaultResult {
        val applicationContext = context.applicationContext
        val updateSetupTokenResult = cardVaultRequest.run {
            paymentMethodTokensAPI.updateSetupToken(
                applicationContext,
                setupTokenId,
                card,
                cardVaultRequest.config
            )
        }

        val authChallengeUrl = updateSetupTokenResult.approveHref
        if (authChallengeUrl == null) {
            val result =
                updateSetupTokenResult.run { CardVaultResult.Success(setupTokenId, status) }
            return result

        } else {
            val returnUrlScheme: String? = Uri.parse(cardVaultRequest.returnUrl).scheme
            val authChallenge = CardAuthChallenge.Vault(
                url = Uri.parse(authChallengeUrl),
                request = cardVaultRequest,
                returnUrlScheme = returnUrlScheme
            )
            return CardVaultResult.AuthorizationRequired(authChallenge)
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: FragmentActivity, authChallenge: CardAuthChallenge) {
//        authChallengeLauncher.presentAuthChallenge(activity, authChallenge)?.let { launchError ->
//            when (authChallenge) {
//                is CardAuthChallenge.ApproveOrder ->
//                    approveOrderListener?.onApproveOrderFailure(launchError)
//
//                is CardAuthChallenge.Vault ->
//                    cardVaultListener?.onVaultFailure(launchError)
//            }
//        }
    }

    internal fun handleBrowserSwitchResult(activity: FragmentActivity) {
//        authChallengeLauncher.deliverBrowserSwitchResult(activity)?.let { status ->
//            when (status) {
//                is CardStatus.VaultSuccess -> notifyVaultSuccess(status.result)
//                is CardStatus.VaultError -> notifyVaultFailure(status.error)
//                is CardStatus.VaultCanceled -> notifyVaultCancelation()
//                is CardStatus.ApproveOrderError ->
//                    notifyApproveOrderFailure(status.error, status.orderId)
//
//                is CardStatus.ApproveOrderSuccess -> notifyApproveOrderSuccess(status.result)
//                is CardStatus.ApproveOrderCanceled -> notifyApproveOrderCanceled(status.orderId)
//            }
//        }
    }

    private fun notifyApproveOrderCanceled(orderId: String?) {
//        analyticsService.sendAnalyticsEvent("card-payments:3ds:challenge:user-canceled", orderId)
//        approveOrderListener?.onApproveOrderCanceled()
    }

    private fun notifyApproveOrderSuccess(result: CardResult) {
//        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", result.orderId)
//        approveOrderListener?.onApproveOrderSuccess(result)
    }

    private fun notifyApproveOrderFailure(error: PayPalSDKError, orderId: String?) {
//        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
//        approveOrderListener?.onApproveOrderFailure(error)
    }

    private fun notifyVaultSuccess(result: CardVaultResult) {
//        analyticsService.sendAnalyticsEvent("card:browser-login:canceled", result.setupTokenId)
//        cardVaultListener?.onVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
//        analyticsService.sendAnalyticsEvent("card:failed", null)
//        cardVaultListener?.onVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
//        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", null)
        // TODO: consider either adding a listener method or next major version returning a result type
//        cardVaultListener?.onVaultFailure(PayPalSDKError(1, "User Canceled"))
    }
}
