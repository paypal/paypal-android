package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.card.CardError
import com.paypal.android.card.CardResponse
import com.paypal.android.card.CardResult
import com.paypal.android.core.*
import org.json.JSONObject
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
        return if (httpResponse.status == 200) {
            val bodyJson =  httpResponse.body?.let { JSONObject(it) } ?: JSONObject()
            val status = bodyJson.getString("status")
            val id = bodyJson.getString("id")
            val paymentSourceCard = bodyJson
                .getJSONObject("payment_source")
                .getJSONObject("card")
            val lastDigits = paymentSourceCard.getString("last_digits")
            val brand = paymentSourceCard.getString("brand")
            val type = paymentSourceCard.getString("type")
            CardResult(response = CardResponse(id, status, lastDigits, brand, type))
        } else {
            CardResult(error = CardError())
        }
    }
}