package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.model.CardResult
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
 */
class CardClient internal constructor(
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val dispatcher: CoroutineDispatcher
) {

    var approveOrderListener: ApproveOrderListener? = null

    private val exceptionHandler = CoreCoroutineExceptionHandler {
        notifyApproveOrderFailure(it)
    }

    private var orderId: String? = null

    /**
     *  CardClient constructor
     *
     *  @param [activity] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     */
    constructor(activity: FragmentActivity, configuration: CoreConfig) :
            this(
                CheckoutOrdersAPI(configuration),
                AnalyticsService(activity.applicationContext, configuration),
                BrowserSwitchClient(),
                Dispatchers.Main
            )

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param activity [FragmentActivity] activity used to start 3DS flow (if requested)
     * @param cardRequest [CardRequest] for requesting an order approval
     */
    fun approveOrder(activity: FragmentActivity, cardRequest: CardRequest) {
        orderId = cardRequest.orderId
        analyticsService.sendAnalyticsEvent("card-payments:3ds:started", orderId)

        CoroutineScope(dispatcher).launch(exceptionHandler) {
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
                    orderId
                )

                // TODO: throw error if parsing fails
                val approveOrderMetadata =
                    ApproveOrderMetadata(cardRequest.orderId, response.paymentSource)
                val authChallenge = CardAuthChallenge(
                    Uri.parse(response.payerActionHref),
                    Uri.parse(cardRequest.returnUrl),
                    approveOrderMetadata
                )
                approveOrderListener?.didReceiveAuthChallenge(authChallenge)
            }
        } catch (error: PayPalSDKError) {
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:confirm-payment-source:failed",
                cardRequest.orderId
            )
            throw error
        }
    }

    private fun notifyApproveOrderSuccess(result: CardResult) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", orderId)
        approveOrderListener?.onApproveOrderSuccess(result)
    }

    private fun notifyApproveOrderFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
        approveOrderListener?.onApproveOrderFailure(error)
    }

    fun continueApproveOrder(authChallengeResult: CardAuthChallengeResult) {
        if (authChallengeResult is CardAuthChallengeSuccess) {
            val metadata = authChallengeResult.approveOrderMetadata
            CoroutineScope(dispatcher).launch(exceptionHandler) {
                try {
                    analyticsService.sendAnalyticsEvent(
                        "card-payments:3ds:get-order-info:succeeded",
                        metadata.orderId
                    )
                    val deepLinkUrl = authChallengeResult.deepLinkUrl
                    val result = CardResult(metadata.orderId, deepLinkUrl)
                    notifyApproveOrderSuccess(result)
                } catch (error: PayPalSDKError) {
                    analyticsService.sendAnalyticsEvent(
                        "card-payments:3ds:get-order-info:failed",
                        metadata.orderId
                    )
                    throw error
                }
            }
        } else if (authChallengeResult is CardAuthChallengeError) {
            // or when cancelled
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:challenge:user-canceled",
                orderId
            )
            approveOrderListener?.onApproveOrderCanceled()
        }
    }
}
