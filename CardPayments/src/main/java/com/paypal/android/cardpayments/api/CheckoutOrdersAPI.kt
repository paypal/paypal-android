package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardRequestFactory
import com.paypal.android.cardpayments.CardResponseParser
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreSDKResult
import com.paypal.android.corepayments.RestClient

internal class CheckoutOrdersAPI(
    private val restClient: RestClient,
    private val requestFactory: CardRequestFactory = CardRequestFactory(),
    private val responseParser: CardResponseParser = CardResponseParser()
) {
    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun confirmPaymentSource(cardRequest: CardRequest): CoreSDKResult<ConfirmPaymentSourceResponse> {
        val apiRequest = requestFactory.createConfirmPaymentSourceRequest(cardRequest)
        return when (val result = restClient.send(apiRequest)) {
            is CoreSDKResult.Success -> {
                val httpResponse = result.value
                val error = responseParser.parseError(httpResponse)
                if (error != null) {
                    CoreSDKResult.Failure(error)
                } else {
                    val successResult =
                        responseParser.parseConfirmPaymentSourceResponse(httpResponse)
                    CoreSDKResult.Success(successResult)
                }
            }

            is CoreSDKResult.Failure -> result
        }
    }
}
