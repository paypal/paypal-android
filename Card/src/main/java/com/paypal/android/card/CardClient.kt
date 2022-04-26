package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(private val cardAPI: CardAPI) {

    constructor(configuration: CoreConfig) :
            this(CardAPI(API(configuration)))

    /**
     * Confirm [Card] payment source for an order. Use this method for Kotlin integrations
     *
     * @param request [CardRequest] for requesting an order approval
     */
    suspend fun approveOrder(request: CardRequest): CardResult =
        request.run { cardAPI.confirmPaymentSource(orderID, card) }

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
}
