package com.paypal.android.card

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
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.card.threedsecure.ThreeDSecureResult
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
    private val activity: FragmentActivity,
    private val cardAPI: CardAPI,
) {

    private val browserSwitchClient = BrowserSwitchClient()
    var approveOrderListener: ApproveOrderListener? = null
    private var confirmPaymentSourceResponse: ConfirmPaymentSourceResponse? = null
    private var lifeCycleObserver: CardLifeCycleObserver? = null
    private var currentCoroutineContext: CoroutineContext? = null

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

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param orderId [String] order id to confirm
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param threeDSecureRequest [threeDSecureRequest] to run transaction with 3DS
     * @param coroutineContext [CoroutineContext] to specify in which context to run api calls
     */
    @JvmOverloads
    fun approveOrder(
        orderId: String,
        cardRequest: CardRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null,
        coroutineContext: CoroutineContext? = Dispatchers.Default
    ) {
        coroutineContext?.also { context ->
            lifeCycleObserver = CardLifeCycleObserver(this)
            lifeCycleObserver?.let { activity.lifecycle.addObserver(it) }
            currentCoroutineContext = context
            CoroutineScope(context).launch {
                try {
                    confirmPaymentSource(orderId, cardRequest, threeDSecureRequest)
                } catch (e: PayPalSDKError) {
                    approveOrderListener?.failure(e)
                    clearClient()
                }
            }
        }
    }

    /**
     * Creates order and confirms [Card] payment source for said order. Its private until phase 2
     *
     * @param orderRequest [String] order to create
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param threeDSecureRequest [threeDSecureRequest] to run transaction with 3DS
     * @param coroutineContext [CoroutineContext] to specify in which context to run api calls
     */
    private fun createAndApproveOrder(
        orderRequest: OrderRequest,
        cardRequest: CardRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null,
        coroutineContext: CoroutineContext? = Dispatchers.Default
    ) {
        coroutineContext?.also { context ->
            lifeCycleObserver = CardLifeCycleObserver(this)
            lifeCycleObserver?.let { activity.lifecycle.addObserver(it) }
            currentCoroutineContext = context
            CoroutineScope(context).launch {
                try {
                    val orderId = createOrder(orderRequest, threeDSecureRequest)
                    confirmPaymentSource(orderId, cardRequest, threeDSecureRequest)
                } catch (e: PayPalSDKError) {
                    approveOrderListener?.failure(e)
                    clearClient()
                }
            }
        }
    }

    private suspend fun createOrder(
        orderRequest: OrderRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ): String {
        val createOrderResponse = cardAPI.createOrder(orderRequest, threeDSecureRequest)
        return createOrderResponse.orderID
    }

    private suspend fun confirmPaymentSource(
        orderId: String,
        cardRequest: CardRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ) {
        val confirmPaymentSourceResponse =
            cardAPI.confirmPaymentSource(orderId, cardRequest.card, threeDSecureRequest)
        if (confirmPaymentSourceResponse.payerActionHref == null) {
            val result = confirmPaymentSourceResponse.let {
                CardResult(it.orderId, it.status, it.paymentSource)
            }
            approveOrderListener?.success(result)
            clearClient()
        } else {
            // we launch the 3DS flow
            val options = BrowserSwitchOptions()
                .url(Uri.parse(confirmPaymentSourceResponse.payerActionHref))
                .returnUrlScheme("com.paypal.android.demo")
            this.confirmPaymentSourceResponse = confirmPaymentSourceResponse
            approveOrderListener?.threeDSecureLaunched()
            browserSwitchClient.start(activity, options)
        }
    }

    internal fun handleBrowserSwitchResult() {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        if (browserSwitchResult != null && approveOrderListener != null && confirmPaymentSourceResponse != null) {
            approveOrderListener?.threeDSecureFinished()
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
            currentCoroutineContext?.also { context ->
                CoroutineScope(context).launch {
                    try {
                        val getOrderResponse =
                            cardAPI.getOrderInfo(GetOrderRequest(response.orderId))
                        approveOrderListener?.success(
                            CardResult(
                                getOrderResponse.orderId,
                                getOrderResponse.orderStatus,
                                getOrderResponse.paymentSource,
                                browserSwitchResult.deepLinkUrl.toString()
                            )
                        )
                    } catch (e: PayPalSDKError) {
                        approveOrderListener?.failure(e)
                    }
                    clearClient()
                }
            }
        }
    }

    private fun deliverCancellation() {
        approveOrderListener?.cancelled()
        clearClient()
    }

    private fun clearClient() {
        currentCoroutineContext = null
        confirmPaymentSourceResponse = null
        approveOrderListener = null
        clearLifeCycleObserver()
    }

    private fun clearLifeCycleObserver() {
        lifeCycleObserver?.let {
            CoroutineScope(Dispatchers.Main).launch {
                //removeObserver() has to be called on main thread
                activity.lifecycle.removeObserver(it)
            }
        }
    }
}
