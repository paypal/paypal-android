package com.paypal.android.card

import com.paypal.android.card.network.CardAPIClient
import com.paypal.android.core.APIClient
import com.paypal.android.core.PaymentsConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CardClient(val configuration: PaymentsConfiguration) {

    private val cardAPI: CardAPIClient = CardAPIClient(APIClient(configuration))

    fun approveOrder(orderId: String, card: Card, completion: (CardResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val cardResult = cardAPI.confirmPaymentSource(orderId, card)
            completion(cardResult)
        }
    }
}
