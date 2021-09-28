package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.PaymentsJSON
import java.net.HttpURLConnection.HTTP_OK

internal class CardAPI(
    private val api: API,
    private val requestBuilder: CardAPIRequestBuilder = CardAPIRequestBuilder()
) {
    suspend fun confirmPaymentSource(orderID: String, card: Card): ConfirmPaymentSourceResult {
        val apiRequest = requestBuilder.buildConfirmPaymentSourceRequest(orderID, card)
        val httpResponse = api.send(apiRequest)
        return if (httpResponse.status == HTTP_OK) {
            runCatching {
                val json = PaymentsJSON(httpResponse.body)
                val status = json.getString("status")
                val id = json.getString("id")
                val lastDigits = json.getString("payment_source.card.last_digits")
                val brand = json.getString("payment_source.card.brand")
                val type = json.getString("payment_source.card.type")
                ConfirmPaymentSourceResult(
                    response = ConfirmedPaymentSource(
                        id,
                        status,
                        lastDigits,
                        brand,
                        type
                    )
                )
            }.recover {
                ConfirmPaymentSourceResult(error = ConfirmPaymentSourceError())
            }.getOrNull()!!
        } else {
            ConfirmPaymentSourceResult(error = ConfirmPaymentSourceError())
        }
    }
}
