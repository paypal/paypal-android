package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardRequestFactory
import com.paypal.android.cardpayments.CardResponseParser
import com.paypal.android.corepayments.RestClient

internal class CheckoutOrdersAPI(
    private val restClient: RestClient,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {
    constructor() : this(RestClient())

    suspend fun confirmPaymentSource(cardRequest: CardRequest.ApproveOrder): ConfirmPaymentSourceResponse {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(cardRequest)
        val httpResponse = restClient.send(apiRequest, cardRequest.config)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
            throw error
        } else {
            return responseParser.parseConfirmPaymentSourceResponse(httpResponse)
        }
    }
}
