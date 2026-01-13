package com.paypal.android.cardpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.paypal.android.cardpayments.analytics.ApproveOrderEvent
import com.paypal.android.cardpayments.analytics.CardAnalytics
import com.paypal.android.cardpayments.analytics.VaultEvent
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
    private val dispatcher: CoroutineDispatcher,
    private val sessionStore: CardSessionStore = CardSessionStore()
) {

    // for analytics tracking
    private var approveOrderId: String? = null
    private var vaultSetupTokenId: String? = null

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
        CardAuthLauncher(context),
        Dispatchers.Main
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

    // NEXT MAJOR VERSION: Consider renaming approveOrder() to confirmPaymentSource()

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param callback [CardApproveOrderCallback] callback for receiving result asynchronously
     */
    fun approveOrder(cardRequest: CardRequest, callback: CardApproveOrderCallback) {
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
                                status = status.name
                            )
                        }
                        callback.onCardApproveOrderResult(result)
                    } else {
                        analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_REQUIRED, approveOrderId)

                        val url = response.payerActionHref.toUri()
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
        vaultSetupTokenId = cardVaultRequest.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId)

        CoroutineScope(dispatcher).launch {
            val updateSetupTokenResult = cardVaultRequest.run {
                paymentMethodTokensAPI.updateSetupToken(setupTokenId, card)
            }
            val result = when (updateSetupTokenResult) {
                is UpdateSetupTokenResult.Success -> {
                    val approveHref = updateSetupTokenResult.approveHref
                    if (approveHref == null) {
                        analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId)
                        updateSetupTokenResult.run { CardVaultResult.Success(setupTokenId, status) }
                    } else {
                        analytics.notify(VaultEvent.AUTH_CHALLENGE_REQUIRED, vaultSetupTokenId)
                        val url = approveHref.toUri()
                        val authChallenge =
                            CardAuthChallenge.Vault(url = url, request = cardVaultRequest)
                        CardVaultResult.AuthorizationRequired(authChallenge)
                    }
                }

                is UpdateSetupTokenResult.Failure -> {
                    analytics.notify(VaultEvent.FAILED, vaultSetupTokenId)
                    CardVaultResult.Failure(updateSetupTokenResult.error)
                }
            }
            callback.onCardVaultResult(result)
        }
    }

    /**
     * Present an auth challenge received from a [CardClient.approveOrder] or [CardClient.vault] result.
     * @param activity [Activity] activity reference used to present a Chrome Custom Tab.
     * @param authChallenge [CardAuthChallenge] auth challenge to present
     * (see [CardApproveOrderResult.AuthorizationRequired])
     */
    fun presentAuthChallenge(
        activity: Activity,
        authChallenge: CardAuthChallenge
    ): CardPresentAuthChallengeResult {
        val result = authChallengeLauncher.presentAuthChallenge(activity, authChallenge)
        captureAuthChallengePresentationAnalytics(result, authChallenge)
        if (result is CardPresentAuthChallengeResult.Success) {
            sessionStore.authState = result.authState
        }
        return result
    }

    private fun captureAuthChallengePresentationAnalytics(
        result: CardPresentAuthChallengeResult,
        authChallenge: CardAuthChallenge
    ) = when (result) {
        is CardPresentAuthChallengeResult.Success -> {
            when (authChallenge) {
                is CardAuthChallenge.ApproveOrder -> analytics.notify(
                    ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    approveOrderId
                )

                is CardAuthChallenge.Vault -> analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId
                )
            }
        }

        is CardPresentAuthChallengeResult.Failure -> {
            when (authChallenge) {
                is CardAuthChallenge.ApproveOrder -> analytics.notify(
                    ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    approveOrderId
                )

                is CardAuthChallenge.Vault -> analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId
                )
            }
        }
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [CardClient.approveOrder]), call this method to see if a user has
     * successfully approved their Credit (or Debit) card as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     * @param [authState] A continuation state received from [CardPresentAuthChallengeResult.Success]
     * when calling [CardClient.presentAuthChallenge]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    @Deprecated(
        message = "Auth state is now captured internally by the SDK. Please migrate to finishApproveOrder(intent).",
        replaceWith = ReplaceWith("finishApproveOrder(intent)")
    )
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

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [CardClient.approveOrder]), call this method to see if a user has
     * successfully approved their Credit (or Debit) card as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishApproveOrder(intent: Intent): CardFinishApproveOrderResult? =
        sessionStore.authState?.let { authState ->
            val result = authChallengeLauncher.completeApproveOrderAuthRequest(intent, authState)
            when (result) {
                is CardFinishApproveOrderResult.Success -> {
                    analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_SUCCEEDED, approveOrderId)
                    sessionStore.clear()
                }

                is CardFinishApproveOrderResult.Failure -> {
                    analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_FAILED, approveOrderId)
                    sessionStore.clear()
                }

                CardFinishApproveOrderResult.Canceled -> {
                    analytics.notify(ApproveOrderEvent.AUTH_CHALLENGE_CANCELED, approveOrderId)
                    sessionStore.clear()
                }

                else -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [CardClient.vault]), call this method to see if a user has
     * successfully authorized their Credit (or Debit) card for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     * @param [authState] A continuation state received from [CardPresentAuthChallengeResult.Success]
     * when calling [CardClient.vault]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    @Deprecated(
        message = "Auth state is now captured internally by the SDK. Please migrate to finishVault(intent).",
        replaceWith = ReplaceWith("finishVault(intent)")
    )
    fun finishVault(intent: Intent, authState: String): CardFinishVaultResult {
        val result = authChallengeLauncher.completeVaultAuthRequest(intent, authState)
        when (result) {
            is CardFinishVaultResult.Success ->
                analytics.notify(VaultEvent.AUTH_CHALLENGE_SUCCEEDED, vaultSetupTokenId)

            is CardFinishVaultResult.Failure ->
                analytics.notify(VaultEvent.AUTH_CHALLENGE_FAILED, vaultSetupTokenId)

            CardFinishVaultResult.Canceled ->
                analytics.notify(VaultEvent.AUTH_CHALLENGE_CANCELED, vaultSetupTokenId)

            else -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [CardClient.vault]), call this method to see if a user has
     * successfully authorized their Credit (or Debit) card for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishVault(intent: Intent): CardFinishVaultResult? =
        sessionStore.authState?.let { authState ->
            val result = authChallengeLauncher.completeVaultAuthRequest(intent, authState)
            when (result) {
                is CardFinishVaultResult.Success -> {
                    analytics.notify(VaultEvent.AUTH_CHALLENGE_SUCCEEDED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                is CardFinishVaultResult.Failure -> {
                    analytics.notify(VaultEvent.AUTH_CHALLENGE_FAILED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                CardFinishVaultResult.Canceled -> {
                    analytics.notify(VaultEvent.AUTH_CHALLENGE_CANCELED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                else -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }
}
