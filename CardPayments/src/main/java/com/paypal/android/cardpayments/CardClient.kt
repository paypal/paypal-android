package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.corepayments.Base64Utils
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import org.json.JSONObject

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
        DataVaultPaymentMethodTokensAPI(context.applicationContext),
        CardAnalytics(context.applicationContext),
        CardAuthLauncher()
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
            analytics.notifyApproveOrderStarted()
            val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
            analytics.notifyConfirmPaymentSourceSucceeded()

            val challengeUrl = response.payerActionHref
            if (challengeUrl == null) {
                analytics.notify3DSSucceeded()
                CardApproveOrderResult.Success(
                    orderId = response.orderId,
                    status = response.status?.name,
                )
            } else {
                analytics.notify3DSChallengeRequired()
                val authChallenge =
                    authLauncher.createAuthChallenge(cardRequest, challengeUrl, analytics)
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
            analytics.notify3DSChallengeRequired()
            val authChallenge =
                authLauncher.createAuthChallenge(cardVaultRequest, challengeUrl, analytics)
            return CardVaultResult.AuthorizationRequired(authChallenge)
        }
    }

    fun checkIfApproveOrderAuthComplete(intent: Intent, state: String): CardApproveOrderAuthResult {
        val authStateJSON =
            decodeCardAuthStateJSON(state) ?: return CardApproveOrderAuthResult.NoResult
        val analytics = restoreAnalyticsContextFromAuthState(authStateJSON)
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

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: FragmentActivity, authChallenge: CardAuthChallenge) =
        authLauncher.presentAuthChallenge(activity, authChallenge)

    fun checkIfVaultAuthComplete(intent: Intent, state: String): CardVaultAuthResult {
        val authStateJSON = decodeCardAuthStateJSON(state) ?: return CardVaultAuthResult.NoResult
        val analytics = restoreAnalyticsContextFromAuthState(authStateJSON)
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

    private fun decodeCardAuthStateJSON(state: String): JSONObject? {
        val authStateJSON = Base64Utils.parseBase64EncodedJSON(state)
        val requestCode = authStateJSON?.optInt("requestCode", -1) ?: -1
        if (requestCode != BrowserSwitchRequestCodes.CARD.intValue) {
            // not a card result
            return null
        }
        return authStateJSON
    }

    private fun restoreAnalyticsContextFromAuthState(stateJSON: JSONObject): CardAnalyticsContext? {
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
}
