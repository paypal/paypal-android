package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.CardAPI
import com.paypal.android.cardpayments.api.GetOrderRequest
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(
    activity: FragmentActivity,
    private val cardAPI: CardAPI,
    private val browserSwitchClient: BrowserSwitchClient,
    private val dispatcher: CoroutineDispatcher
) {

    var approveOrderListener: ApproveOrderListener? = null

    private val lifeCycleObserver = CardLifeCycleObserver(this)

    private val exceptionHandler = CoreCoroutineExceptionHandler {
        approveOrderListener?.onApproveOrderFailure(it)
    }
    /**
     *  CardClient constructor
     *
     *  @param [activity] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     */
    constructor(activity: FragmentActivity, configuration: CoreConfig) :
            this(
                activity,
                CardAPI(API(configuration, activity)),
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
        CoroutineScope(dispatcher).launch(exceptionHandler) {
            confirmPaymentSource(activity, cardRequest)
        }
    }

    private suspend fun confirmPaymentSource(activity: FragmentActivity, cardRequest: CardRequest) {
        val response = cardAPI.confirmPaymentSource(cardRequest)
        if (response.payerActionHref == null) {
            val result = response.run {
                CardResult(orderID, status, paymentSource)
            }
            approveOrderListener?.onApproveOrderSuccess(result)
        } else {
            approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()

            // launch the 3DS flow
            val urlScheme = cardRequest.run { Uri.parse(returnUrl).scheme }
            val approveOrderMetadata =
                ApproveOrderMetadata(cardRequest.orderID, response.paymentSource)
            val options = BrowserSwitchOptions()
                .url(Uri.parse(response.payerActionHref))
                .returnUrlScheme(urlScheme)
                .metadata(approveOrderMetadata.toJSON())

            browserSwitchClient.start(activity, options)
        }
    }

    internal fun handleBrowserSwitchResult(activity: FragmentActivity) {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        if (browserSwitchResult != null && approveOrderListener != null) {
            approveOrderListener?.onApproveOrderThreeDSecureDidFinish()
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> getOrderInfo(browserSwitchResult)
                BrowserSwitchStatus.CANCELED -> notifyApproveOrderCanceled()
            }
        }
    }

    private fun getOrderInfo(browserSwitchResult: BrowserSwitchResult) {
        ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)?.let { metadata ->
            CoroutineScope(dispatcher).launch(exceptionHandler) {
                val getOrderResponse = cardAPI.getOrderInfo(GetOrderRequest(metadata.orderID))
                approveOrderListener?.onApproveOrderSuccess(
                    CardResult(
                        getOrderResponse.orderId,
                        getOrderResponse.orderStatus,
                        getOrderResponse.paymentSource,
                        browserSwitchResult.deepLinkUrl
                    )
                )
            }
        }
    }

    private fun notifyApproveOrderCanceled() {
        approveOrderListener?.onApproveOrderCanceled()
    }
}
