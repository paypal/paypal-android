package com.paypal.android.card

import com.paypal.android.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.coroutines.CoroutineContext

class CardClient(val paymentsClient: PaymentsClient) {

    private val http = Http()
    private val baseUrl = Environment.SANDBOX.url

    fun approveOrder(orderId: String, card: Card, completion: (CardResult) -> Unit) {

        val confirmPaymentSourceUrl = "${baseUrl}/v2/checkout/orders/${orderId}/confirm-payment-source"
        val body = """
            {
                "payment_source": {
                    "card": {
                        "number":"${card.number}",
                        "expiry":"${card.expiry}"
                    }
                }
            }
        """.trimIndent()

        val request = HttpRequest(URL(confirmPaymentSourceUrl), "POST", body)
        CoroutineScope(Dispatchers.Main).launch {
            val clientToken = fetchClientToken()
            request.headers["Authorization"] = "Basic ${clientToken}"

            val response = http.send(request)
            //response to CardResult
            //
            completion(CardResult(null, Object()))
        }


        //paymentsClients.approveOrder(orderId, PaymentSource(card))
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
