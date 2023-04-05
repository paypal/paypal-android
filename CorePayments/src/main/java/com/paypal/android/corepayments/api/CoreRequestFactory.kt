package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.api.models.GetOrderRequest

open class CoreRequestFactory {

    fun createGetOrderInfoRequest(getOrderRequest: GetOrderRequest): APIRequest {
        val path = "v2/checkout/orders/${getOrderRequest.orderId}"
        return APIRequest(path, HttpMethod.GET, "")
    }
}
