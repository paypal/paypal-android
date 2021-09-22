package com.paypal.android.card

import com.paypal.android.card.network.CardAPIClient
import com.paypal.android.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CardClient(val paymentConfig: PaymentsConfiguration) {

    private val apiClient = CardAPIClient()

//    fun approveOrder(orderId: String, card: Card, completion: (CardResult) -> Unit) {
//        CoroutineScope(Dispatchers.Main).launch {
//            val cardResult = apiClient.confirmPaymentSource(paymentConfig, orderId, card)
//            completion(cardResult)
//        }
//    }

    fun approveOrder(orderId: String, card: Card, completion: (Result<CardResult>) -> Unit) {

    }
}
