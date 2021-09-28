package com.paypal.android.card

import com.paypal.android.core.APIClient
import com.paypal.android.core.PaymentsConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to approve an order with a [Card].
 */
class CardClient internal constructor(
    private val cardAPI: CardAPIClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    constructor(configuration: PaymentsConfiguration) :
            this(CardAPIClient(APIClient(configuration)))

    /**
     * Confirm [Card] payment source for an order.
     *
     * @param orderID The id of the order
     * @param card The card to use for approval
     * @param completion A completion handler for receiving a [ConfirmPaymentSourceResult]
     */
    fun confirmPaymentSource(
        orderID: String,
        card: Card,
        completion: (ConfirmPaymentSourceResult) -> Unit
    ) {
        CoroutineScope(dispatcher).launch {
            val cardResult = cardAPI.confirmPaymentSource(orderID, card)
            completion(cardResult)
        }
    }
}
