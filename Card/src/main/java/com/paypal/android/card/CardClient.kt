package com.paypal.android.card

import com.paypal.android.core.APIClient
import com.paypal.android.core.PaymentsConfiguration

class CardClient(private val apiClient: APIClient) {

//    private val apiClient = CardAPIClient()

    constructor(clientId: String) : this(APIClient(PaymentsConfiguration(clientId)))

//    fun approveOrder(orderId: String, card: Card, completion: (CardResult) -> Unit) {
//        CoroutineScope(Dispatchers.Main).launch {
//            val cardResult = apiClient.confirmPaymentSource(paymentConfig, orderId, card)
//            completion(cardResult)
//        }
//    }

    suspend fun approveOrder(orderId: String, card: Card): CardOrderApproval {
        val path = "v2/checkout/orders/${orderId}/confirm-payment-source"
        val body = """
            {
                "payment_source": {
                    "card": {
                        "number": "${card.number}",
                        "expiry": "${card.expiry}"
                    }
                }
            }
        """.trimIndent()

        val httpResponse = apiClient.post(path, body)
        return CardOrderApproval(httpResponse, null)
    }
}
