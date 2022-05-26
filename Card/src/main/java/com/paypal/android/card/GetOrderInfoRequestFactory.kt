package com.paypal.android.card

import com.paypal.android.card.api.GetOrderInfoResponse
import com.paypal.android.card.api.GetOrderRequest
import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.core.APIClientError
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import com.paypal.android.core.containsKey
import org.json.JSONException

internal object GetOrderInfoRequestFactory {

    fun createRequest(getOrderRequest: GetOrderRequest): APIRequest {
        val path = "v2/checkout/orders/${getOrderRequest.orderId}"
        return APIRequest(path, HttpMethod.GET, "")
    }

    @Throws(PayPalSDKError::class)
    fun parseResponse(response: String, correlationId: String?): GetOrderInfoResponse =
        try {
            val json = PaymentsJSON(response)
            GetOrderInfoResponse(json)
        } catch (e: JSONException) {
            throw APIClientError.dataParsingError(correlationId, e)
        }
}
