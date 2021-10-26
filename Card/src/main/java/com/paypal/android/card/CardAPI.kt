package com.paypal.android.card

import android.webkit.URLUtil
import com.paypal.android.core.API
import com.paypal.android.core.HttpResponse.Companion.SERVER_ERROR
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNDETERMINED
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNKNOWN_HOST
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PaymentsJSON
import java.net.HttpURLConnection.HTTP_OK

internal class CardAPI(
    private val api: API,
    private val requestFactory: CardAPIRequestFactory = CardAPIRequestFactory()
) {
    suspend fun confirmPaymentSource(orderID: String, card: Card): CardResult {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(orderID, card)
        val httpResponse = api.send(apiRequest)
        val correlationId = httpResponse.headers["Paypal-Debug-Id"]
        if (URLUtil.isValidUrl(apiRequest.path).not()) {
            CardResult.Error(
                payPalSDKError = APIClientError.invalidUrlRequest,
                correlationID = correlationId
            )
        }
        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            return CardResult.Error(
                payPalSDKError = APIClientError.noResponseData,
                correlationID = correlationId
            )
        }
        return when (httpResponse.status) {
            HTTP_OK -> {
                runCatching {
                    val json = PaymentsJSON(bodyResponse)
                    val status = json.getString("status")
                    val id = json.getString("id")

                    CardResult.Success(
                        orderID = id,
                        status = OrderStatus.valueOf(status)
                    )
                }.recover {
                    CardResult.Error(
                        payPalSDKError = APIClientError.dataParsingError,
                        correlationID = correlationId
                    )
                }.getOrNull()!!
            }
            STATUS_UNKNOWN_HOST -> {
                CardResult.Error(
                    payPalSDKError = APIClientError.unknownHost,
                    correlationID = correlationId
                )
            }
            STATUS_UNDETERMINED -> {
                CardResult.Error(
                    payPalSDKError = APIClientError.unknownError,
                    correlationID = correlationId
                )
            }
            SERVER_ERROR -> {
                CardResult.Error(
                    payPalSDKError = APIClientError.serverResponseError,
                    correlationID = correlationId
                )
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

                CardResult.Error(
                    payPalSDKError = APIClientError.httpURLConnectionError(httpResponse.status, "$message -> $errorDetails"),
                    correlationID = correlationId
                )
            }
        }
    }
}
