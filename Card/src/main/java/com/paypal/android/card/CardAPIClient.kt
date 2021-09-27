package com.paypal.android.card

import com.paypal.android.core.APIClient
import com.paypal.android.core.PayPalJSON
import java.net.HttpURLConnection.HTTP_OK

class CardAPIClient(
    private val api: APIClient,
    private val requestBuilder: CardAPIRequestBuilder = CardAPIRequestBuilder()
) {
    suspend fun confirmPaymentSource(orderId: String, card: Card): ConfirmPaymentSourceResult {
        val apiRequest = requestBuilder.buildConfirmPaymentSourceRequest(orderId, card)
        val httpResponse = api.send(apiRequest)
        return if (httpResponse.status == HTTP_OK) {
            val json = PayPalJSON(httpResponse.body)

            val status = json.optString("status")
            val id = json.optString("id")
            val lastDigits = json.optString("payment_source.card.last_digits")
            val brand = json.optString("payment_source.card.brand")
            val type = json.optString("payment_source.card.type")

            ConfirmPaymentSourceResult(response = ConfirmedPaymentSource(id, status, lastDigits, brand, type))
        } else {
            ConfirmPaymentSourceResult(error = ConfirmPaymentSourceError())
        }
    }
}
