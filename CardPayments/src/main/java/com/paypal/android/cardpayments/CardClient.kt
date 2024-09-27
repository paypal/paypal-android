package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.corepayments.PayPalSDKError

/**
 * Use this client to approve an order with a [Card].
 *
 */
class CardClient internal constructor(
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val cardAnalytics: CardAnalytics,
    private val authLauncher: CardAuthLauncher
) {

    constructor(context: Context) : this(context, CardAnalytics(context.applicationContext))

    internal constructor(context: Context, cardAnalytics: CardAnalytics) : this(
        CheckoutOrdersAPI(),
        DataVaultPaymentMethodTokensAPI(context.applicationContext),
        cardAnalytics,
        CardAuthLauncher(cardAnalytics)
    )

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()
    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardApproveOrderRequest] for requesting an order approval
     */
    suspend fun approveOrder(cardRequest: CardApproveOrderRequest): CardApproveOrderResult {
        val analytics = cardAnalytics.createAnalyticsContext(cardRequest)

        return try {
            analytics.notifyConfirmPaymentSourceStarted()
            val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)

            val challengeUrl = response.payerActionHref
            if (challengeUrl == null) {
                analytics.notifyConfirmPaymentSourceSucceeded()
                CardApproveOrderResult.Success(
                    orderId = response.orderId,
                    status = response.status?.name,
                )
            } else {
                analytics.notifyConfirmPaymentSourceSCARequired()
                val authChallenge =
                    authLauncher.createAuthChallenge(cardRequest, challengeUrl, analytics.trackingId)
                CardApproveOrderResult.AuthorizationRequired(authChallenge)
            }
        } catch (error: PayPalSDKError) {
            // TODO: migrate away from throwing exceptions to result objects
            analytics.notifyConfirmPaymentSourceFailed()
            CardApproveOrderResult.Failure(error)
        }
    }

    /**
     * @suppress
     *
     * Call this method to attach a payment source to a setup token.
     *
     * @param cardVaultRequest [CardVaultRequest] request containing details about the setup token
     * and card to use for vaulting.
     */
    suspend fun vault(cardVaultRequest: CardVaultRequest): CardVaultResult {
        val analytics = cardAnalytics.createAnalyticsContext(cardVaultRequest)
        val updateSetupTokenResult = cardVaultRequest.run {
            paymentMethodTokensAPI.updateSetupToken(setupTokenId, card, cardVaultRequest.config)
        }

        val challengeUrl = updateSetupTokenResult.approveHref
        if (challengeUrl == null) {
            analytics.notifyCardVault3DSSuccess()
            return updateSetupTokenResult.run { CardVaultResult.Success(setupTokenId, status) }
        } else {
            analytics.notifyConfirmPaymentSourceSCARequired()
            val authChallenge =
                authLauncher.createAuthChallenge(cardVaultRequest, challengeUrl, analytics.trackingId)
            return CardVaultResult.AuthorizationRequired(authChallenge)
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: FragmentActivity, authChallenge: CardAuthChallenge) =
        authLauncher.presentAuthChallenge(activity, authChallenge)

    fun checkIfApproveOrderAuthComplete(intent: Intent, state: String): CardApproveOrderAuthResult =
        authLauncher.checkIfApproveOrderAuthComplete(intent, state)

    fun checkIfVaultAuthComplete(intent: Intent, state: String): CardVaultAuthResult =
        authLauncher.checkIfVaultAuthComplete(intent, state)
}
