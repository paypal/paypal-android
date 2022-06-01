package com.paypal.android.card.api

import com.paypal.android.card.Card
import com.paypal.android.card.CardRequest
import com.paypal.android.card.CardRequestFactory
import com.paypal.android.card.CardResponseParser
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.core.API
import com.paypal.android.core.APIClientError

internal class CardAPI(
    private val api: API,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {

    suspend fun confirmPaymentSource(
        cardRequest: CardRequest
    ): ConfirmPaymentSourceResponse {
        val apiRequest = cardRequest.run {
            requestFactory.createConfirmPaymentSourceRequest(orderID, card, threeDSecureRequest)
        }

        val httpResponse = api.send(apiRequest)
        val correlationID = httpResponse.headers["Paypal-Debug-Id"]

        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }

        if (httpResponse.isSuccessful) {
            return responseParser.parseConfirmPaymentSourceResponse(bodyResponse, correlationID)
        } else {
            throw responseParser.parseError(httpResponse.status, bodyResponse, correlationID)
        }
    }

    suspend fun getOrderInfo(
        getOrderRequest: GetOrderRequest,
    ): GetOrderInfoResponse {
        val apiRequest = requestFactory.createGetOrderInfoRequest(getOrderRequest)

        val httpResponse = api.send(apiRequest)
        val correlationID = httpResponse.headers["Paypal-Debug-Id"]

        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }

        if (httpResponse.isSuccessful) {
            return responseParser.parseGetOrderInfoResponse(bodyResponse, correlationID)
        } else {
            throw responseParser.parseError(httpResponse.status, bodyResponse, correlationID)
        }
    }
}
