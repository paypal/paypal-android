package com.paypal.android.threedsecure

import com.paypal.android.card.Card
import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
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

        val bodyResponse = httpResponse.body

        val status = httpResponse.status
        if (status == HTTP_OK) {
            return parseThreeDSecureResult(bodyResponse!!, null)
        }

        throw APIClientError.noResponseData("what")
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
            throw APIClientError.noResponseData("what")
//            throw APIClientError.dataParsingError(correlationID)
        }.getOrNull()!!
}