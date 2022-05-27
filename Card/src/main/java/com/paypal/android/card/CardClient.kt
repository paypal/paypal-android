package com.paypal.android.card

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.card.api.CardAPI
import com.paypal.android.card.api.ConfirmPaymentSourceResponse
import com.paypal.android.card.api.GetOrderRequest
import com.paypal.android.card.model.CardResult
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(
    activity: FragmentActivity,
    private val cardAPI: CardAPI,
) {

    var approveOrderListener: ApproveOrderListener? = null

    private val browserSwitchClient = BrowserSwitchClient()
    private val lifeCycleObserver = CardLifeCycleObserver(this)

    // TODO: determine if we can store this in browser switch metadata to support process kill
    private var confirmPaymentSourceResponse: ConfirmPaymentSourceResponse? = null

    /**
     *  CardClient constructor
     *
     *  @param [activity] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     */
    constructor(
        activity: FragmentActivity,
        configuration: CoreConfig,
    ) : this(activity, CardAPI(API(configuration)))

    init {
        activity.lifecycle.addObserver(lifeCycleObserver)
    }

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param orderId [String] order id to confirm
     * @param cardRequest [CardRequest] for requesting an order approval
     */
    @JvmOverloads
    fun approveOrder(
        activity: FragmentActivity,
        orderId: String,
        cardRequest: CardRequest,
        context: CoroutineContext = Dispatchers.IO
    ) {
        CoroutineScope(context).launch {
            try {
                confirmPaymentSource(activity, orderId, cardRequest)
            } catch (e: PayPalSDKError) {
                approveOrderListener?.onApproveOrderFailure(e)
                clearClient()
            }
        }
    }

    private suspend fun confirmPaymentSource(
        activity: FragmentActivity,
        orderId: String,
        cardRequest: CardRequest
    ) {
        val confirmPaymentSourceResponse =
            cardAPI.confirmPaymentSource(orderId, cardRequest.card, cardRequest.threeDSecureRequest)
        if (confirmPaymentSourceResponse.payerActionHref == null) {
            val result = confirmPaymentSourceResponse.let {
                CardResult(it.orderId, it.status, it.paymentSource)
            }
            approveOrderListener?.onApproveOrderSuccess(result)
            clearClient()
        } else {
            // we launch the 3DS flow
            val options = BrowserSwitchOptions()
                .url(Uri.parse(confirmPaymentSourceResponse.payerActionHref))
                .returnUrlScheme("com.paypal.android.demo")
            this.confirmPaymentSourceResponse = confirmPaymentSourceResponse
            approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()
            browserSwitchClient.start(activity, options)
        }
    }

    internal fun handleBrowserSwitchResult(activity: FragmentActivity) {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        if (browserSwitchResult != null && approveOrderListener != null && confirmPaymentSourceResponse != null) {
            approveOrderListener?.onApproveOrderThreeDSecureDidFinish()
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> getOrderInfo(
                    browserSwitchResult,
                    confirmPaymentSourceResponse
                )
                BrowserSwitchStatus.CANCELED -> deliverCancellation()
            }
        }
    }

    private fun getOrderInfo(
        browserSwitchResult: BrowserSwitchResult,
        confirmPaymentSourceResponse: ConfirmPaymentSourceResponse?
    ) {
        confirmPaymentSourceResponse?.also { response ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val getOrderResponse =
                        cardAPI.getOrderInfo(GetOrderRequest(response.orderId))
                    approveOrderListener?.onApproveOrderSuccess(
                        CardResult(
                            getOrderResponse.orderId,
                            getOrderResponse.orderStatus,
                            confirmPaymentSourceResponse.paymentSource,
                            browserSwitchResult.deepLinkUrl
                        )
                    )
                } catch (e: PayPalSDKError) {
                    approveOrderListener?.onApproveOrderFailure(e)
                }
                clearClient()
            }
        }
    }

    private fun deliverCancellation() {
        approveOrderListener?.onApproveOrderCanceled()
        clearClient()
    }

    private fun clearClient() {
        confirmPaymentSourceResponse = null
        approveOrderListener = null
    }
}
