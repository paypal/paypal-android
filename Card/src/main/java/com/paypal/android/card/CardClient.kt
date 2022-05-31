package com.paypal.android.card

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.card.api.CardAPI
import com.paypal.android.card.api.GetOrderRequest
import com.paypal.android.card.model.CardResult
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                withContext(Dispatchers.Main) {
                    approveOrderListener?.onApproveOrderFailure(e)
                }
            }
        }
    }

    private suspend fun confirmPaymentSource(
        activity: FragmentActivity,
        orderId: String,
        cardRequest: CardRequest
    ) {
        val response =
            cardAPI.confirmPaymentSource(orderId, cardRequest.card, cardRequest.threeDSecureRequest)
        if (response.payerActionHref == null) {
            val result = response.run {
                CardResult(orderID, status, paymentSource)
            }
            withContext(Dispatchers.Main) {
                approveOrderListener?.onApproveOrderSuccess(result)
            }
        } else {
            withContext(Dispatchers.Main) {
                approveOrderListener?.onApproveOrderThreeDSecureWillLaunch()
            }

            // launch the 3DS flow
            val approveOrderMetadata = ApproveOrderMetadata(orderId, response.paymentSource)
            val options = BrowserSwitchOptions()
                .url(Uri.parse(response.payerActionHref))
                .returnUrlScheme("com.paypal.android.demo")
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
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val getOrderResponse =
                        cardAPI.getOrderInfo(GetOrderRequest(metadata.orderId))
                    approveOrderListener?.onApproveOrderSuccess(
                        CardResult(
                            getOrderResponse.orderId,
                            getOrderResponse.orderStatus,
                            metadata.paymentSource,
                            browserSwitchResult.deepLinkUrl
                        )
                    )
                } catch (e: PayPalSDKError) {
                    approveOrderListener?.onApproveOrderFailure(e)
                }
            }
        }
    }

    private fun notifyApproveOrderCanceled() {
        approveOrderListener?.onApproveOrderCanceled()
    }
}
