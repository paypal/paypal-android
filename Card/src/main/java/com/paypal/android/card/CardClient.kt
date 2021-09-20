package com.paypal.android.card

import com.paypal.android.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.coroutines.CoroutineContext

class CardClient(val paymentsClient: PaymentsClient) {

    fun approveOrder(orderId: String, card: Card, completion: (CardResult) -> Unit) {
        val url = "/v2/checkout/orders/${orderId}/confirm-payment-source"
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
        val finalURL = Environment.SANDBOX.url + url;
        val request = HttpRequest(URL(finalURL), "POST", body)
        val http = Http()
        CoroutineScope(Dispatchers.Main).launch {
            val response = http.send(request)
            //response to CardResult
            //
            completion(CardResult(null, Object()))
        }


        //paymentsClients.approveOrder(orderId, PaymentSource(card))
    }
}
