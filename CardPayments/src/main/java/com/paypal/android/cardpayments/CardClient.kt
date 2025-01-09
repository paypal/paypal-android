package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.paypal.android.cardpayments.analytics.ApproveOrderEvent
import com.paypal.android.cardpayments.analytics.CardAnalytics
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 *
 */
class CardClient internal constructor(
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val paymentMethodTokensAPI: DataVaultPaymentMethodTokensAPI,
    private val analytics: CardAnalytics,
    private val authChallengeLauncher: CardAuthLauncher,
    private val dispatcher: CoroutineDispatcher
) {

    private var approveOrderId: String? = null

    /**
     *  CardClient constructor
     *
     *  @param [context] Android context
     *  @param [configuration] Configuration parameters for client
     */
    constructor(context: Context, configuration: CoreConfig) : this(
        CheckoutOrdersAPI(configuration),
        DataVaultPaymentMethodTokensAPI(context.applicationContext, configuration),
        CardAnalytics(AnalyticsService(context.applicationContext, configuration)),
        CardAuthLauncher(),
        Dispatchers.Main
    )

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param callback [CardApproveOrderCallback] callback for receiving result asynchronously
     */
    fun approveOrder(cardRequest: CardRequest, callback: CardApproveOrderCallback) {
        // TODO: deprecate this method and offer auth challenge integration pattern (similar to vault)
        approveOrderId = cardRequest.orderId
        analytics.notify(ApproveOrderEvent.STARTED, approveOrderId)

        CoroutineScope(dispatcher).launch {
            when (val response = checkoutOrdersAPI.confirmPaymentSource(cardRequest)) {
                is ConfirmPaymentSourceResult.Success -> {
                    if (response.payerActionHref == null) {
                        analytics.notify(ApproveOrderEvent.SUCCEEDED, approveOrderId)
                        val result = response.run {
                            CardApproveOrderResult.Success(
                                orderId = orderId,
                                status = status?.name
                            )
                        }
                        callback.onCardApproveOrderResult(result)
                    } else {
                        analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_REQUIRED, approveOrderId)

                        val url = Uri.parse(response.payerActionHref)
                        val authChallenge = CardAuthChallenge.ApproveOrder(url, cardRequest)
                        val result = CardApproveOrderResult.AuthorizationRequired(authChallenge)
                        callback.onCardApproveOrderResult(result)
                    }
                }

                is ConfirmPaymentSourceResult.Failure -> {
                    analytics.notify(ApproveOrderEvent.FAILED, approveOrderId)
                    val result = CardApproveOrderResult.Failure(response.error)
                    callback.onCardApproveOrderResult(result)
                }
            }
        }
    }

    /**
     * @suppress
     *
     * Call this method to attach a payment source to a setup token.
     *
     * @param cardVaultRequest [CardVaultRequest] request containing details about the setup token
     * and card to use for vaulting.
     * @param callback [CardVaultCallback] callback for receiving a [CardVaultResult] asynchronously
     */
    fun vault(cardVaultRequest: CardVaultRequest, callback: CardVaultCallback) {
        analytics.notifyVaultStarted(cardVaultRequest.setupTokenId)

        CoroutineScope(dispatcher).launch {
            val updateSetupTokenResult = cardVaultRequest.run {
                paymentMethodTokensAPI.updateSetupToken(setupTokenId, card)
            }
            val result = when (updateSetupTokenResult) {
                is UpdateSetupTokenResult.Success -> {
                    val approveHref = updateSetupTokenResult.approveHref
                    if (approveHref == null) {
                        analytics.notifyVaultSucceeded(updateSetupTokenResult.setupTokenId)
                        updateSetupTokenResult.run { CardVaultResult.Success(setupTokenId, status) }
                    } else {
                        analytics.notifyVaultAuthChallengeReceived(updateSetupTokenResult.setupTokenId)
                        val url = Uri.parse(approveHref)
                        val authChallenge =
                            CardAuthChallenge.Vault(url = url, request = cardVaultRequest)
                        CardVaultResult.AuthorizationRequired(authChallenge)
                    }
                }

                is UpdateSetupTokenResult.Failure -> {
                    analytics.notifyVaultFailed(cardVaultRequest.setupTokenId)
                    CardVaultResult.Failure(updateSetupTokenResult.error)
                }
            }
            callback.onCardVaultResult(result)
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     * @param activity [ComponentActivity] activity reference used to present a Chrome Custom Tab.
     * @param authChallenge [CardAuthChallenge] auth challenge to present
     * (see [CardApproveOrderResult.AuthorizationRequired])
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
                    analytics.notify(
                        ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                        approveOrderId
                    )

                // TODO: see if we can get setup token from somewhere
                is CardAuthChallenge.Vault ->
                    analytics.notifyVaultAuthChallengeStarted(null)
            }
        }

        is CardPresentAuthChallengeResult.Failure -> {
            when (authChallenge) {
                // TODO: see if we can get order id from somewhere
                is CardAuthChallenge.ApproveOrder ->
                    analytics.notify(
                        ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                        approveOrderId
                    )

                // TODO: see if we can get setup token id from somewhere
                is CardAuthChallenge.Vault ->
                    analytics.notifyVaultAuthChallengeFailed(null)
            }
        }
    }

    fun finishApproveOrder(intent: Intent, authState: String): CardFinishApproveOrderResult {
        val result = authChallengeLauncher.completeApproveOrderAuthRequest(intent, authState)
        when (result) {
            is CardFinishApproveOrderResult.Success ->
                analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_SUCCEEDED, approveOrderId)

            is CardFinishApproveOrderResult.Failure ->
                analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_FAILED, approveOrderId)

            CardFinishApproveOrderResult.Canceled ->
                analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_CANCELED, approveOrderId)

            else -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    fun finishVault(intent: Intent, authState: String): CardFinishVaultResult {
        val result = authChallengeLauncher.completeVaultAuthRequest(intent, authState)
        when (result) {
            is CardFinishVaultResult.Success ->
                analytics.notifyVaultAuthChallengeSucceeded(result.setupTokenId)

            // TODO: see if we can access setup token id for analytics tracking
            is CardFinishVaultResult.Failure ->
                analytics.notifyVaultAuthChallengeFailed(null)

            // TODO: see if we can access setup token id for analytics tracking
            CardFinishVaultResult.Canceled ->
                analytics.notifyVaultAuthChallengeCanceled(null)

            else -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }
}
