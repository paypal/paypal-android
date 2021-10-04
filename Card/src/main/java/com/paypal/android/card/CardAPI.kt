package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.OrderData
import com.paypal.android.core.OrderError
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PaymentsJSON
import java.net.HttpURLConnection.HTTP_OK

internal class CardAPI(
    private val api: API,
    private val requestFactory: CardAPIRequestFactory = CardAPIRequestFactory()
) {
    suspend fun confirmPaymentSource(orderID: String, card: Card): ConfirmPaymentSourceResult {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(orderID, card)
        val httpResponse = api.send(apiRequest)
        return if (httpResponse.status == HTTP_OK) {
            runCatching {
                val json = PaymentsJSON(httpResponse.body)
                val status = json.getString("status")
                val id = json.getString("id")
                ConfirmPaymentSourceResult(response = OrderData(id, OrderStatus.valueOf(status)))
            }.recover {
                ConfirmPaymentSourceResult(error = OrderError())
            }.getOrNull()!!
        } else {
            ConfirmPaymentSourceResult(error = OrderError())
        }
    }
}
