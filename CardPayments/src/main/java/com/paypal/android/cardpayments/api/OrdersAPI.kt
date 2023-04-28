package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardRequestFactory
import com.paypal.android.cardpayments.CardResponseParser
import com.paypal.android.corepayments.RestClient

internal class OrdersAPI(
    private val restClient: RestClient,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {

    suspend fun confirmPaymentSource(cardRequest: CardRequest): ConfirmPaymentSourceResponse {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(cardRequest)
        val httpResponse = restClient.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
//            sendAnalyticsEvent("card-payments:3ds:confirm-payment-source:failed")
            throw error
        } else {
//            sendAnalyticsEvent("card-payments:3ds:confirm-payment-source:succeeded")
            return responseParser.parseConfirmPaymentSourceResponse(httpResponse)
        }
    }

    suspend fun getOrderInfo(getOrderRequest: GetOrderRequest): GetOrderInfoResponse {
        val apiRequest = requestFactory.createGetOrderInfoRequest(getOrderRequest)
        val httpResponse = restClient.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
//            sendAnalyticsEvent("card-payments:3ds:get-order-info:failed")
            throw error
        } else {
//            sendAnalyticsEvent("card-payments:3ds:get-order-info:succeeded")
            return responseParser.parseGetOrderInfoResponse(httpResponse)
        }
    }
}
