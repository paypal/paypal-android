package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.card.CardOrderApproval
import com.paypal.android.core.ConfirmPaymentSourceRequestFactory
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequest
import com.paypal.android.core.PaymentsConfiguration
import java.net.URL

class CardAPIClient(private val config: PaymentsConfiguration) {
    private val paymentSourceRequestFactory = ConfirmPaymentSourceRequestFactory(config)
    private val http = Http()

    suspend fun confirmPaymentSource(orderId: String, card: Card): CardOrderApproval {
        val request = paymentSourceRequestFactory.create(orderId, card.toJson())
        val clientToken = fetchClientToken(config)
        request.headers["Authorization"] = "Basic $clientToken"

        val response = http.send(request)
        //response to CardResult
        return CardOrderApproval(response, null)
    }

    private suspend fun fetchClientToken(config: PaymentsConfiguration): String {
        // fetch lsat
        val fetchClientTokenUrl = URL("${config.environment.url}/v1/oauth2/token")
        val body = """
            {
                "grant_type": "client_credentials",
                "response_type": "id_token"
            }
        """.trimIndent()

        val request = HttpRequest(fetchClientTokenUrl, "POST", body)

        val orderID = "sample-order-id"
        request.headers["Authorization"] = "Basic ${orderID}"

        return http.send(request).body
    }
}