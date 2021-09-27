package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.card.CardError
import com.paypal.android.card.CardResponse
import com.paypal.android.card.CardResult
import com.paypal.android.core.APIClient
import com.paypal.android.core.PayPalJSON
import java.net.HttpURLConnection.HTTP_OK

class CardAPIClient(
    private val api: APIClient,
    private val requestBuilder: CardAPIRequestBuilder
) {
    suspend fun confirmPaymentSource(orderId: String, card: Card): CardResult {
        val apiRequest = requestBuilder.buildConfirmPaymentSourceRequest(orderId, card)
        val httpResponse = api.send(apiRequest)
        return if (httpResponse.status == HTTP_OK) {
            val json = PayPalJSON(httpResponse.body)

            val status = json.optString("status")
            val id = json.optString("id")
            val lastDigits = json.optString("payment_source.card.last_digits")
            val brand = json.optString("payment_source.card.brand")
            val type = json.optString("payment_source.card.type")

            CardResult(response = CardResponse(id, status, lastDigits, brand, type))
        } else {
            CardResult(error = CardError())
        }
    }
}
