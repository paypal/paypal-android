package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
    private val analytics: CardAnalytics,
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

    // TODO: remove once try-catch is removed
    private val approveOrderExceptionHandler = CoreCoroutineExceptionHandler { error ->
        analytics.notifyApproveOrderUnknownError(null)
        approveOrderListener?.onApproveOrderFailure(error)
    }

    // TODO: remove once try-catch is removed
    private val vaultExceptionHandler = CoreCoroutineExceptionHandler { error ->
        analytics.notifyVaultUnknownError(null)
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
        CardAnalytics(AnalyticsService(context.applicationContext, configuration)),
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
        analytics.notifyApproveOrderStarted(cardRequest.orderId)

        CoroutineScope(dispatcher).launch(approveOrderExceptionHandler) {
            // TODO: migrate away from throwing exceptions to result objects
            try {
                val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)
                if (response.payerActionHref == null) {
                    analytics.notifyApproveOrderSucceeded(response.orderId)
                    val result =
                        response.run { CardResult(orderId = orderId, status = status?.name) }
                    approveOrderListener?.onApproveOrderSuccess(result)
                } else {
                    analytics.notifyApproveOrderAuthChallengeReceived(cardRequest.orderId)
                    approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()

                    val url = Uri.parse(response.payerActionHref)
                    val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
                    approveOrderListener?.onApproveOrderAuthorizationRequired(authChallenge)
                }
            } catch (error: PayPalSDKError) {
                analytics.notifyApproveOrderFailed(cardRequest.orderId)
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
        analytics.notifyVaultStarted(cardVaultRequest.setupTokenId)

        val applicationContext = context.applicationContext
        CoroutineScope(dispatcher).launch(vaultExceptionHandler) {
            try {
                val updateSetupTokenResult = cardVaultRequest.run {
                    paymentMethodTokensAPI.updateSetupToken(applicationContext, setupTokenId, card)
                }

                val approveHref = updateSetupTokenResult.approveHref
                if (approveHref == null) {
                    analytics.notifyVaultSucceeded(updateSetupTokenResult.setupTokenId)
                    val result = updateSetupTokenResult.run { CardVaultResult(setupTokenId, status) }
                    cardVaultListener?.onVaultSuccess(result)
                } else {
                    analytics.notifyVaultAuthChallengeReceived(updateSetupTokenResult.setupTokenId)
                    val url = Uri.parse(approveHref)
                    val authChallenge = CardAuthChallenge.Vault(url = url, request = cardVaultRequest)
                    cardVaultListener?.onVaultAuthorizationRequired(authChallenge)
                }

            } catch (error: PayPalSDKError) {
                analytics.notifyVaultFailed(cardVaultRequest.setupTokenId)
                throw error
            }
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(activity: ComponentActivity, authChallenge: CardAuthChallenge) {
        when (authChallengeLauncher.presentAuthChallenge(activity, authChallenge)) {
            is CardPresentAuthChallengeResult.Success -> {
                when (authChallenge) {
                    // TODO: see if we can get order id from somewhere
                    is CardAuthChallenge.ApproveOrder ->
                        analytics.notifyApproveOrderAuthChallengeStarted(null)

                    // TODO: see if we can get setup token from somewhere
                    is CardAuthChallenge.Vault ->
                        analytics.notifyVaultAuthChallengeStarted(null)
                }
            }

            is CardPresentAuthChallengeResult.Failure -> {
                when (authChallenge) {
                    // TODO: see if we can get order id from somewhere
                    is CardAuthChallenge.ApproveOrder ->
                        analytics.notifyApproveOrderAuthChallengeFailed(null)

                    // TODO: see if we can get setup token id from somewhere
                    is CardAuthChallenge.Vault ->
                        analytics.notifyVaultAuthChallengeFailed(null)
                }
            }
        }
    }

    fun completeAuthChallenge(intent: Intent, authState: String): CardStatus {
        val status = authChallengeLauncher.completeAuthRequest(intent, authState)
        when (status) {
            is CardStatus.VaultSuccess -> {
                analytics.notifyVaultAuthChallengeSucceeded(status.result.setupTokenId)
                cardVaultListener?.onVaultSuccess(status.result)
            }
            is CardStatus.VaultError -> {
                // TODO: see if we can access setup token id for analytics tracking
                analytics.notifyVaultAuthChallengeFailed(null)
                cardVaultListener?.onVaultFailure(status.error)
            }
            is CardStatus.VaultCanceled -> {
                // TODO: see if we can access setup token id for analytics tracking
                analytics.notifyVaultAuthChallengeCanceled(null)
                // TODO: consider either adding a listener method or next major version returning a result type
                cardVaultListener?.onVaultFailure(PayPalSDKError(1, "User Canceled"))
            }
            is CardStatus.ApproveOrderError -> {
                analytics.notifyApproveOrderAuthChallengeFailed(status.orderId)
                approveOrderListener?.onApproveOrderFailure(status.error)
            }

            is CardStatus.ApproveOrderSuccess -> {
                analytics.notifyApproveOrderAuthChallengeSucceeded(status.result.orderId)
                approveOrderListener?.onApproveOrderSuccess(status.result)
            }

            is CardStatus.ApproveOrderCanceled -> {
                analytics.notifyApproveOrderAuthChallengeCanceled(status.orderId)
                approveOrderListener?.onApproveOrderCanceled()
            }

            is CardStatus.UnknownError -> {
                Log.d("PayPalSDK", "An unknown error occurred: ${status.error.message}")
            }

            CardStatus.NoResult -> {
                // ignore
            }
        }
        return status
    }

    /**
     * Call this method at the end of the card flow to clear out all observers and listeners
     */
    fun removeObservers() {
        approveOrderListener = null
        cardVaultListener = null
    }
}
