package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig
import kotlinx.coroutines.CoroutineDispatcher
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
     * Confirm [Card] payment source for an order.
     *
     * @param request [CardRequest] for requesting an order approval
     */
    suspend fun approveOrder(request: CardRequest): CardResult =
        cardAPI.confirmPaymentSource(request.orderID, request.card)
}
