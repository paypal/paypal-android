package com.paypal.android.card

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(private val cardAPI: CardAPI) {

    private val browserSwitchClient = BrowserSwitchClient()

    constructor(configuration: CoreConfig) :
            this(CardAPI(API(configuration)))

    /**
     * Confirm [Card] payment source for an order. Use this method for Kotlin integrations
     *
     * @param request [CardRequest] for requesting an order approval
     */
    suspend fun approveOrder(request: CardRequest): CardResult =
        cardAPI.confirmPaymentSource(request.orderID, request.card)

    /**
     * Confirm [Card] payment source for an order. Use this method for Java integrations
     *
     * @param request [CardRequest] for requesting an order approval
     * @param callback callback to get response
     */
    fun approveOrder(request: CardRequest, callback: ApproveOrderCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = approveOrder(request)
                callback.success(result)
            } catch (e: PayPalSDKError) {
                callback.failure(e)
            }
        }
    }

    suspend fun verifyCard(activity: FragmentActivity, orderID: String, card: Card) {
        val threedsHref = "https://www.sandbox.paypal.com/webapps/helios?action=verify&flow=3ds&cart_id=4EF77104WD360373D"
//        val threedsHref = cardAPI.verifyCard(orderID, card)
        val options = BrowserSwitchOptions()
            .url(Uri.parse(threedsHref))
            .returnUrlScheme("com.paypal.android.demo")
        browserSwitchClient.start(activity, options)
    }
}
