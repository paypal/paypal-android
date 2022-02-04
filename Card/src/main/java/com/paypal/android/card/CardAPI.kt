package com.paypal.android.card

import android.webkit.URLUtil
import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
import com.paypal.android.core.HttpResponse.Companion.SERVER_ERROR
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNDETERMINED
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNKNOWN_HOST
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import java.net.HttpURLConnection.HTTP_OK

internal class CardAPI(
    private val api: API,
    private val requestFactory: CardAPIRequestFactory = CardAPIRequestFactory()
) {
    @Throws(PayPalSDKError::class)
    suspend fun confirmPaymentSource(orderID: String, card: Card): CardResult {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(orderID, card)
        val httpResponse = api.send(apiRequest)
        val correlationID = httpResponse.headers["Paypal-Debug-Id"]
        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }
        return when (httpResponse.status) {
            HTTP_OK -> {
                runCatching {
                    val json = PaymentsJSON(bodyResponse)
                    val status = json.getString("status")
                    val id = json.getString("id")

                    CardResult(id, OrderStatus.valueOf(status))
                }.recover {
                    throw APIClientError.dataParsingError(correlationID)
                }.getOrNull()!!
            }
            STATUS_UNKNOWN_HOST -> {
                throw APIClientError.unknownHost(correlationID)
            }
            STATUS_UNDETERMINED -> {
                throw APIClientError.unknownError(correlationID)
            }
            SERVER_ERROR -> {
                throw APIClientError.serverResponseError(correlationID)
            }
            else -> {
                val json = PaymentsJSON(bodyResponse)
                val message = json.getString("message")

                val errorDetails = mutableListOf<OrderErrorDetail>()
                val errorDetailsJson = json.getJSONArray("details")
                for (i in 0 until errorDetailsJson.length()) {
                    val errorJson = errorDetailsJson.getJSONObject(i)
                    val issue = errorJson.getString("issue")
                    val description = errorJson.getString("description")
                    errorDetails += OrderErrorDetail(issue, description)
                }

                val description = "$message -> $errorDetails"
                throw APIClientError.httpURLConnectionError(
                    httpResponse.status,
                    description,
                    correlationID
                )
            }
        }
    }
}
