package com.paypal.android.card

import com.paypal.android.card.network.CardAPIClient
import com.paypal.android.core.APIClient
import com.paypal.android.core.PaymentsConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CardClient(configuration: PaymentsConfiguration) {

    private val cardAPI: CardAPIClient = CardAPIClient(APIClient(configuration))

    fun approveOrder(orderID: String, card: Card, completion: (CardResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val cardResult = cardAPI.confirmPaymentSource(orderID, card)
            completion(cardResult)
        }
    }
}
