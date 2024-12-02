package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardRequestFactory
import com.paypal.android.cardpayments.CardResponseParser
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.RestClient

internal class CheckoutOrdersAPI(
    private val restClient: RestClient,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {
    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun confirmPaymentSource(cardRequest: CardRequest): ConfirmPaymentSourceResult {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(cardRequest)
        val httpResponse = restClient.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        return if (error == null) {
            responseParser.parseConfirmPaymentSourceResponse(httpResponse)
        } else {
            ConfirmPaymentSourceResult.Failure(error)
        }
    }
}
