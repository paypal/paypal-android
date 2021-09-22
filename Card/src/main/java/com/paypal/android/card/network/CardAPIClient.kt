package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.card.CardResult
import com.paypal.android.core.*
import java.net.URL

class CardAPIClient(private val api: APIClient) {

    suspend fun confirmPaymentSource(orderId: String, card: Card): CardResult {
        val path = "v2/checkout/orders/${orderId}/confirm-payment-source"
        val body = """
            {
                "payment_source": {
                    "card": {
                        "number": "${card.number}",
                        "expiry": "${card.expiry}"
                    }
                }
            }
        """.trimIndent()

        val httpResponse = api.post(path, body)
        return CardResult(httpResponse, null)
    }
}