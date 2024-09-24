package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.corepayments.Base64Utils
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
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

    /**
     *  CardClient constructor
     *
     *  @param [context] Activity that launches the card client
     */
    constructor(context: Context) : this(
        CheckoutOrdersAPI(),
        DataVaultPaymentMethodTokensAPI(),
        CardAnalytics(context.applicationContext),
        CardAuthLauncher()
    )

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()
    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardApproveOrderRequest] for requesting an order approval
     */
    suspend fun approveOrder(cardRequest: CardRequest.ApproveOrder): CardApproveOrderResult {
        val analytics = cardAnalytics.createAnalyticsContext(cardRequest)

        return try {
            analytics.notifyApproveOrderStarted()
            val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
            analytics.notifyConfirmPaymentSourceSucceeded()

            val payerActionHref = response.payerActionHref
            if (payerActionHref == null) {
                analytics.notify3DSSucceeded()
                CardApproveOrderResult.Success(
                    orderId = response.orderId,
                    status = response.status?.name,
                    didAttemptThreeDSecureAuthentication = false
                )
            } else {
                analytics.notify3DSChallengeRequired()

                val authChallenge = CardAuthChallenge.create(cardRequest, payerActionHref)
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
     * @param context [Context] Android context reference
     * @param cardVaultRequest [CardVaultRequest] request containing details about the setup token
     * and card to use for vaulting.
     */
    suspend fun vault(context: Context, cardVaultRequest: CardRequest.Vault): CardVaultResult {
        val applicationContext = context.applicationContext
        val updateSetupTokenResult = cardVaultRequest.run {
            paymentMethodTokensAPI.updateSetupToken(
                applicationContext,
                setupTokenId,
                card,
                cardVaultRequest.config
            )
        }

        val approveHref = updateSetupTokenResult.approveHref
        if (approveHref == null) {
            return updateSetupTokenResult.run { CardVaultResult.Success(setupTokenId, status) }
        } else {
            val authChallenge = CardAuthChallenge.create(cardVaultRequest, approveHref)
            return CardVaultResult.AuthorizationRequired(authChallenge)
        }
    }

    private fun parseTrackingContextFromAuthChallengeState(state: String): CardAnalyticsContext? {
        val stateJSON = Base64Utils.parseBase64EncodedJSON(state) ?: return null
        val metadata = stateJSON.optJSONObject("metadata")
        val clientId = metadata?.optString("client_id")
        val environmentName = metadata?.optString("environment_name")
        val environment = try {
            Environment.valueOf(environmentName ?: "")
        } catch (e: IllegalArgumentException) {
            null
        }

        val orderId = metadata?.optString("order_id")
        val setupTokenId = metadata?.optString("setup_token_id")
        if (clientId.isNullOrEmpty() || environment == null) {
            return null
        }
        val coreConfig = CoreConfig(clientId, environment)
        return cardAnalytics.createAnalyticsContext(coreConfig, orderId, setupTokenId)
    }

    fun checkIfApproveOrderAuthComplete(intent: Intent, state: String): CardApproveOrderAuthResult {
        val analytics = parseTrackingContextFromAuthChallengeState(state)
        val result = authLauncher.checkIfApproveOrderAuthComplete(intent, state)
        when (result) {
            is CardApproveOrderAuthResult.Success -> analytics?.notify3DSSucceeded()
            is CardApproveOrderAuthResult.Failure -> analytics?.notify3DSFailed()
            CardApproveOrderAuthResult.NoResult -> {
                // do nothing
            }
        }
        return result
    }

    fun checkIfVaultAuthComplete(intent: Intent, state: String): CardVaultAuthResult {
        val analytics = parseTrackingContextFromAuthChallengeState(state)
        val result = authLauncher.checkIfVaultAuthComplete(intent, state)
        when (result) {
            is CardVaultAuthResult.Success -> analytics?.notifyCardVault3DSSuccess()
            is CardVaultAuthResult.Failure -> analytics?.notifyCardVault3DSFailure()
            CardVaultAuthResult.NoResult -> {
                // do nothing
            }
        }
        return result
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: FragmentActivity, authChallenge: CardAuthChallenge) =
        authLauncher.presentAuthChallenge(activity, authChallenge)
}
