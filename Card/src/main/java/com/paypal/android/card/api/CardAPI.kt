package com.paypal.android.card.api

import com.paypal.android.card.CardRequest
import com.paypal.android.card.CardRequestFactory
import com.paypal.android.card.CardResponseParser
import com.paypal.android.core.API

internal class CardAPI(
    private val api: API,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {

    suspend fun confirmPaymentSource(cardRequest: CardRequest): ConfirmPaymentSourceResponse {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(cardRequest)
        val httpResponse = api.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
            throw error
        } else {
            return responseParser.parseConfirmPaymentSourceResponse(httpResponse)
        }
    }

    suspend fun getOrderInfo(getOrderRequest: GetOrderRequest): GetOrderInfoResponse {
        val apiRequest = requestFactory.createGetOrderInfoRequest(getOrderRequest)
        val httpResponse = api.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
            throw error
        } else {
            return responseParser.parseGetOrderInfoResponse(httpResponse)
        }
    }
}
