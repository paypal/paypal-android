package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.card.CardResult
import com.paypal.android.core.Environment
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequest
import java.net.URL

class CardAPIClient {
    private val http = Http()
    private val baseUrl = Environment.SANDBOX.url

    suspend fun confirmPaymentSource(orderId: String, card: Card): CardResult {
        val request = ConfirmPaymentSourceRequestFactory().create(orderId, card)
        val clientToken = fetchClientToken()
        request.headers["Authorization"] = "Basic $clientToken"

        val response = http.send(request)
            //response to CardResult
        return CardResult(response, null)
    }

    private suspend fun fetchClientToken(): String {
        // fetch lsat
        val fetchClientTokenUrl = URL("${baseUrl}/v1/oauth2/token")
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