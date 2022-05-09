package com.paypal.android.card

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.card.api.CardAPI
import com.paypal.android.card.api.ConfirmPaymentSourceResponse
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.card.threedsecure.ThreeDSecureResult
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(
    private val activity: FragmentActivity,
    private val cardAPI: CardAPI,
    private val coroutineContext: CoroutineContext
) {

    private val browserSwitchClient = BrowserSwitchClient()
    private var approveOrderCallback: ApproveOrderCallback? = null
    private var confirmPaymentSourceResponse: ConfirmPaymentSourceResponse? = null

    /**
     *  CardClient constructor
     *
     *  @param [activity] Activity that launches the card client
     *  @param [configuration] Configuration parameters for client
     *  @param [coroutineContext] CoroutineContext to execute
     */
    @JvmOverloads
    constructor(activity: FragmentActivity, configuration: CoreConfig, coroutineContext: CoroutineContext = Dispatchers.Main) :
            this(activity, CardAPI(API(configuration)), coroutineContext)

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param orderId [String] order id to confirm
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param threeDSecureRequest [threeDSecureRequest] to run transaction with 3DS
     * @param callback [ApproveOrderCallback] callback for responses
     */
    @JvmOverloads
    fun approveOrder(orderId: String, cardRequest: CardRequest, threeDSecureRequest: ThreeDSecureRequest? = null, callback: ApproveOrderCallback) {
        CoroutineScope(coroutineContext).launch {
            confirmPaymentSource(orderId, cardRequest, threeDSecureRequest, callback)
        }
    }

    /**
     * Creates order and confirms [Card] payment source for said order.
     *
     * @param orderRequest [String] order to create
     * @param cardRequest [CardRequest] for requesting an order approval
     * @param threeDSecureRequest [threeDSecureRequest] to run transaction with 3DS
     * @param callback [ApproveOrderCallback] callback for responses
     */
    @JvmOverloads
    fun createAndApproveOrder(orderRequest: OrderRequest, cardRequest: CardRequest, threeDSecureRequest: ThreeDSecureRequest? = null, callback: ApproveOrderCallback) {
        CoroutineScope(coroutineContext).launch {
            val orderId = createOrder(orderRequest, threeDSecureRequest)
            confirmPaymentSource(orderId, cardRequest, threeDSecureRequest, callback)
        }
    }

    private suspend fun createOrder(orderRequest: OrderRequest, threeDSecureRequest: ThreeDSecureRequest? = null): String {
        val createOrderResponse = cardAPI.createOrder(orderRequest, threeDSecureRequest)
        return createOrderResponse.orderID
    }

    private suspend fun confirmPaymentSource(orderId: String, cardRequest: CardRequest, threeDSecureRequest: ThreeDSecureRequest? = null, callback: ApproveOrderCallback) {
        try {
            val confirmPaymentSourceResponse =
                cardAPI.confirmPaymentSource(orderId, cardRequest.card, threeDSecureRequest)
            if (confirmPaymentSourceResponse.payerActionHref == null) {
                val result = confirmPaymentSourceResponse.let {
                    CardResult(it.orderId, it.status)
                }
                callback.success(result)
            } else {
                // we launch the 3DS flow
                val options = BrowserSwitchOptions()
                    .url(Uri.parse(confirmPaymentSourceResponse.payerActionHref))
                    .returnUrlScheme("com.paypal.android.demo") // get this ref from return_url
                approveOrderCallback = callback
                activity.lifecycle.addObserver(CardLifeCycleObserver(this@CardClient))
                browserSwitchClient.start(activity, options)
            }
        } catch (e:PayPalSDKError) {
            callback.failure(e)
        }
    }

    internal fun handleBrowserSwitchResult() {
        var result = browserSwitchClient.deliverResult(activity)
        approveOrderCallback?.let { callback ->
            confirmPaymentSourceResponse?.let { response ->
                //here we need to parse the browser switcht result with liability_shift
                callback.success(CardResult(response.orderId, response.status, ThreeDSecureResult("NO")))
            }
        }
    }
}
