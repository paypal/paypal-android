package com.paypal.android.card
import com.paypal.android.card.api.CreateOrderResponse
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.core.APIClientError
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import org.json.JSONException

internal object CreateOrderRequestFactory {

    fun createRequest(
        orderRequest: OrderRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ): APIRequest {
        // remove threeDSecure once confirm_payment_source supports it
        val path = "v2/checkout/orders"
        val body = """
            {
                "intent": "${orderRequest.intent}",
                "purchase_units": [
                    ${
            orderRequest.purchaseUnits?.let {
                it.joinToString { purchaseUnit ->
                    """
                                    {
                                        "reference_id": "${purchaseUnit.referenceId}",
                                        "amount": {
                                            "currency_code": "${purchaseUnit.amount.currencyCode}",
                                            "value": "${purchaseUnit.amount.value}"
                                        }
                                    }
                                """
                }
            }
        }
                ]${
            threeDSecureRequest?.let {
                """
                            ,
                            "application_context": {
                                "return_url": "${it.returnUrl}",
                                "cancel_url": "${it.cancelUrl}"
                            }
                        """
            } ?: ""
        }
            }
        """.trimIndent()
        return APIRequest(path, HttpMethod.POST, body)
    }

    @Throws(PayPalSDKError::class)
    fun parseResponse(response: String, correlationId: String?): CreateOrderResponse =
        try {
            val json = PaymentsJSON(response)
            val status = json.getString("status")
            val id = json.getString("id")
            CreateOrderResponse(id, OrderStatus.valueOf(status))
        } catch (e: JSONException) {
            throw APIClientError.dataParsingError(correlationId, e)
        }
}
