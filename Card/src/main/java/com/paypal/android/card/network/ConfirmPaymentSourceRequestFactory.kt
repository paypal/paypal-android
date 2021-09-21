package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.core.Environment
import com.paypal.android.core.HttpRequest
import java.net.URL

class ConfirmPaymentSourceRequestFactory(private val baseUrl: String = Environment.SANDBOX.url) {
    //inject variable enviorment or url variable?

    fun create(orderId: String, card: Card): HttpRequest {
        val confirmPaymentSourceUrl = "${baseUrl}/v2/checkout/orders/${orderId}/confirm-payment-source"
        val body = """
            {
                "payment_source": {
                    "card": {
                        "number":"${card.number}",
                        "expiry":"${card.expiry}"
                    }
                }
            }
        """.trimIndent()

        return HttpRequest(URL(confirmPaymentSourceUrl), "POST", body)
    }
}