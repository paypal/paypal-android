package com.paypal.android.threedsecure

import com.paypal.android.card.Card
import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import java.net.HttpURLConnection.HTTP_OK

internal class ThreeDSecureAPI(
    private val api: API,
    private val requestFactory: ThreeDSecureAPIRequestFactory = ThreeDSecureAPIRequestFactory()
) {

    @Throws(PayPalSDKError::class)
    suspend fun verifyCard(orderID: String, card: Card): ThreeDSecureResult {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(orderID, card)
        val httpResponse = api.send(apiRequest)
        val correlationID = httpResponse.headers["Paypal-Debug-Id"]

        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }

        val status = httpResponse.status
        return if (status == HTTP_OK) {
            parseThreeDSecureResult(bodyResponse, correlationID)
        } else {
            throw parseThreeDSecureError(status, bodyResponse, correlationID)
        }
    }

    private fun parseThreeDSecureResult(bodyResponse: String, correlationID: String?): ThreeDSecureResult =
        runCatching {
            val json = PaymentsJSON(bodyResponse)
            val status = json.getString("status")
            val id = json.getString("id")

            val linksArray = json.getJSONArray("links")
            val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }

            val payerActionLink = links.first { it.getString("rel") == "payer-action" }
            val payerActionHref = payerActionLink.getString("href")

            ThreeDSecureResult(id, OrderStatus.valueOf(status), payerActionHref)
        }.recover {
            throw APIClientError.dataParsingError(correlationID)
        }.getOrNull()!!

    private fun parseThreeDSecureError(
        status: Int,
        bodyResponse: String,
        correlationID: String?
    ) = when (status) {
        HttpResponse.STATUS_UNKNOWN_HOST -> {
            APIClientError.unknownHost(correlationID)
        }
        HttpResponse.STATUS_UNDETERMINED -> {
            APIClientError.unknownError(correlationID)
        }
        HttpResponse.SERVER_ERROR -> {
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