package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 *
 * @property cardVaultListener listener to receive callbacks form [CardClient.vault].
 */
class CardClient internal constructor(
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val analytics: CardAnalytics,
    private val authChallengeLauncher: CardAuthLauncher,
    private val dispatcher: CoroutineDispatcher
) {

    // NEXT MAJOR VERSION: rename to vaultListener
    /**
     * @suppress
     */
    var cardVaultListener: CardVaultListener? = null

    private var approveOrderId: String? = null

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
    fun approveOrder(cardRequest: CardRequest, callback: CardCallback.ApproveOrder) {
        // TODO: deprecate this method and offer auth challenge integration pattern (similar to vault)
        approveOrderId = cardRequest.orderId
        analytics.notifyApproveOrderStarted(cardRequest.orderId)

        CoroutineScope(dispatcher).launch {
            when (val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)) {
                is ConfirmPaymentSourceResult.Success -> {
                    if (response.payerActionHref == null) {
                        analytics.notifyApproveOrderSucceeded(response.orderId)
                        val result: CardResult.ApproveOrder = response.run {
                            CardResult.ApproveOrder.Success(
                                orderId = orderId,
                                status = status?.name
                            )
                        }
                        callback.onApproveOrderResult(result)
                    } else {
                        analytics.notifyApproveOrderAuthChallengeReceived(cardRequest.orderId)

                        val url = Uri.parse(response.payerActionHref)
                        val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
                        val result = CardResult.ApproveOrder.AuthorizationRequired(authChallenge)
                        callback.onApproveOrderResult(result)
                    }
                }

                is ConfirmPaymentSourceResult.Failure -> {
                    analytics.notifyApproveOrderFailed(cardRequest.orderId)
                    val result = CardResult.ApproveOrder.Failure(response.error)
                    callback.onApproveOrderResult(result)
                }
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
        CoroutineScope(dispatcher).launch {
            val updateSetupTokenResult = cardVaultRequest.run {
                paymentMethodTokensAPI.updateSetupToken(applicationContext, setupTokenId, card)
            }
            when (updateSetupTokenResult) {
                is UpdateSetupTokenResult.Success -> {
                    val approveHref = updateSetupTokenResult.approveHref
                    if (approveHref == null) {
                        analytics.notifyVaultSucceeded(updateSetupTokenResult.setupTokenId)
                        val result =
                            updateSetupTokenResult.run { CardVaultResult(setupTokenId, status) }
                        cardVaultListener?.onVaultSuccess(result)
                    } else {
                        analytics.notifyVaultAuthChallengeReceived(updateSetupTokenResult.setupTokenId)
                        val url = Uri.parse(approveHref)
                        val authChallenge =
                            CardAuthChallenge.Vault(url = url, request = cardVaultRequest)
                        cardVaultListener?.onVaultAuthorizationRequired(authChallenge)
                    }
                }

                is UpdateSetupTokenResult.Failure -> {
                    analytics.notifyVaultFailed(cardVaultRequest.setupTokenId)
                    cardVaultListener?.onVaultFailure(updateSetupTokenResult.error)
                }
            }
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     */
    fun presentAuthChallenge(
        activity: ComponentActivity,
        authChallenge: CardAuthChallenge
    ): CardPresentAuthChallengeResult {
        val result = authChallengeLauncher.presentAuthChallenge(activity, authChallenge)
        captureAuthChallengePresentationAnalytics(result, authChallenge)
        return result
    }

    private fun captureAuthChallengePresentationAnalytics(
        result: CardPresentAuthChallengeResult,
        authChallenge: CardAuthChallenge
    ) = when (result) {
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

    fun finishApproveOrder(intent: Intent, authState: String): CardResult.FinishApproveOrder {
        val result = authChallengeLauncher.completeApproveOrderAuthRequest(intent, authState)
        when (result) {
            is CardResult.FinishApproveOrder.Success ->
                analytics.notifyApproveOrderAuthChallengeSucceeded(result.orderId)

            is CardResult.FinishApproveOrder.Failure ->
                analytics.notifyApproveOrderAuthChallengeFailed(null)

            CardResult.FinishApproveOrder.Canceled ->
                analytics.notifyApproveOrderAuthChallengeCanceled(null)

            else -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    fun legacyCompleteAuthChallenge(intent: Intent, authState: String): CardStatus {
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

            is CardStatus.UnknownError -> {
                Log.d("PayPalSDK", "An unknown error occurred: ${status.error.message}")
            }

            else -> {
                // ignore
            }
        }
        return status
    }

    /**
     * Call this method at the end of the card flow to clear out all observers and listeners
     */
    fun removeObservers() {
        cardVaultListener = null
    }
}
