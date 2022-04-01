package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
import com.paypal.android.core.HttpResponse.Companion.SERVER_ERROR
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNDETERMINED
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNKNOWN_HOST
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import org.json.JSONObject
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

        val status = httpResponse.status
        if (status == HTTP_OK) {
            return parseCardResult(bodyResponse, correlationID)
        } else {
            throw parseCardError(status, bodyResponse, correlationID)
        }
    }

    suspend fun verifyCard(orderID: String, card: Card): String {
        val apiRequest = requestFactory.createAuthorizeWith3DSVerificationRequest(orderID, card)
        val httpResponse = api.send(apiRequest)

        val responseJSON = JSONObject(httpResponse.body)

        val linksArray = responseJSON.getJSONArray("links")
        val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }

        val payerActionLink = links.first { it.getString("rel") == "payer-action" }
        return payerActionLink.getString("href")
    }

    private fun parseCardResult(bodyResponse: String, correlationID: String?): CardResult =
        runCatching {
            val json = PaymentsJSON(bodyResponse)
            val status = json.getString("status")
            val id = json.getString("id")

            CardResult(id, OrderStatus.valueOf(status))
        }.recover {
            throw APIClientError.dataParsingError(correlationID)
        }.getOrNull()!!

    private fun parseCardError(
        status: Int,
        bodyResponse: String,
        correlationID: String?
    ) = when (status) {
        STATUS_UNKNOWN_HOST -> {
            APIClientError.unknownHost(correlationID)
        }
        STATUS_UNDETERMINED -> {
            APIClientError.unknownError(correlationID)
        }
        SERVER_ERROR -> {
            APIClientError.serverResponseError(correlationID)
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
            APIClientError.httpURLConnectionError(
                status,
                description,
                correlationID
            )
        }
    }
}
