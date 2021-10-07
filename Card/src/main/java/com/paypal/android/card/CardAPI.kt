package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.OrderData
import com.paypal.android.core.OrderError
import com.paypal.android.core.OrderErrorDetail
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
                ConfirmPaymentSourceResult(error = OrderError("PARSING_ERROR", "Error parsing json response."))
            }.getOrNull()!!
        } else {
            val json = PaymentsJSON(httpResponse.body)
            val name = json.getString("name")
            val message = json.getString("message")

            val errorDetails = mutableListOf<OrderErrorDetail>()
            val errorDetailsJson = json.getJSONArray("details")
            for (i in 0 until errorDetailsJson.length()) {
                val errorJson = errorDetailsJson.getJSONObject(i)
                val issue = errorJson.getString("issue")
                val description = errorJson.getString("description")
                errorDetails += OrderErrorDetail(issue, description)
            }

            ConfirmPaymentSourceResult(error = OrderError(name, message, errorDetails))
        }
    }
}
