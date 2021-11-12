package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.CoreConfig

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(private val cardAPI: CardAPI) {

    constructor(configuration: CoreConfig) :
            this(CardAPI(API(configuration)))

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param orderID The id of the order
     * @param card The card to use for approval
     * @return [CardResult]
     */
    suspend fun approveOrder(orderID: String, card: Card) =
        cardAPI.confirmPaymentSource(orderID, card)
}
