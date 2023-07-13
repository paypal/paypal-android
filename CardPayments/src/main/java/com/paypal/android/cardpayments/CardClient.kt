package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.VaultPaymentMethodTokensAPI
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
    activity: FragmentActivity,
    private val checkoutOrdersAPI: CheckoutOrdersAPI,
    private val vaultPaymentMethodTokensAPI: VaultPaymentMethodTokensAPI,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val dispatcher: CoroutineDispatcher
) {

    var approveOrderListener: ApproveOrderListener? = null
    var vaultListener: VaultListener? = null

    private val lifeCycleObserver = CardLifeCycleObserver(this)

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
                activity,
                CheckoutOrdersAPI(configuration),
                VaultPaymentMethodTokensAPI(configuration),
                AnalyticsService(activity.applicationContext, configuration),
                BrowserSwitchClient(),
                Dispatchers.Main
            )

    init {
        activity.lifecycle.addObserver(lifeCycleObserver)
    }

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

    /**
     * Initiate vaulting for a Card payment method.
     *
     * NOTE: 3DS Challenge will occur when required.
     */
    fun vault(vaultRequest: VaultRequest) {
        CoroutineScope(dispatcher).launch {
            val result = vaultPaymentMethodTokensAPI.createSetupToken(vaultRequest)
            vaultListener?.onVaultSuccess(result)
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
                approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()

                // launch the 3DS flow
                val urlScheme = cardRequest.run { Uri.parse(returnUrl).scheme }
                val approveOrderMetadata =
                    ApproveOrderMetadata(cardRequest.orderId, response.paymentSource)
                val options = BrowserSwitchOptions()
                    .url(Uri.parse(response.payerActionHref))
                    .returnUrlScheme(urlScheme)
                    .metadata(approveOrderMetadata.toJSON())

                browserSwitchClient.start(activity, options)
            }
        } catch (error: PayPalSDKError) {
            analyticsService.sendAnalyticsEvent(
                "card-payments:3ds:confirm-payment-source:failed",
                cardRequest.orderId
            )
            throw error
        }
    }

    internal fun handleBrowserSwitchResult(activity: FragmentActivity) {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        if (browserSwitchResult != null && approveOrderListener != null) {
            approveOrderListener?.onApproveOrderThreeDSecureDidFinish()
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> handleBrowserSwitchSuccess(browserSwitchResult)
                BrowserSwitchStatus.CANCELED -> notifyApproveOrderCanceled()
            }
        }
    }

    private fun handleBrowserSwitchSuccess(browserSwitchResult: BrowserSwitchResult) {
        ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)?.let { metadata ->
            CoroutineScope(dispatcher).launch(exceptionHandler) {
                try {
                    analyticsService.sendAnalyticsEvent(
                        "card-payments:3ds:get-order-info:succeeded",
                        metadata.orderId
                    )
                    val deepLinkUrl = browserSwitchResult.deepLinkUrl
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
        }
    }

    private fun notifyApproveOrderCanceled() {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:challenge:user-canceled", orderId)
        approveOrderListener?.onApproveOrderCanceled()
    }

    private fun notifyApproveOrderSuccess(result: CardResult) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", orderId)
        approveOrderListener?.onApproveOrderSuccess(result)
    }

    private fun notifyApproveOrderFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
        approveOrderListener?.onApproveOrderFailure(error)
    }
}
