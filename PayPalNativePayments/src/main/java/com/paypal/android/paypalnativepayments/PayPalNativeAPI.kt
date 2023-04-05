package com.paypal.android.paypalnativepayments

import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.api.CoreRequestFactory
import com.paypal.android.corepayments.api.CoreRequestParser
import com.paypal.android.corepayments.api.models.GetOrderInfoResponse
import com.paypal.android.corepayments.api.models.GetOrderRequest

internal class PayPalNativeAPI(
    private val api: API,
    private val requestFactory: CoreRequestFactory = CoreRequestFactory(),
    private val responseParser: CoreRequestParser = CoreRequestParser()
) {

    internal suspend fun getOrderInfo(getOrderRequest: GetOrderRequest): GetOrderInfoResponse {
        val apiRequest = requestFactory.createGetOrderInfoRequest(getOrderRequest)
        val httpResponse = api.send(apiRequest)

        val error = responseParser.parseError(httpResponse)
        if (error != null) {
            sendAnalyticsEvent("paypal-native-payments:shipping-method-changed:get-order-info:failed")
            throw error
        } else {
            sendAnalyticsEvent("paypal-native-payments:shipping-method-changed:get-order-info:succeeded")
            return responseParser.parseGetOrderInfoResponse(httpResponse)
        }
    }

    internal suspend fun fetchCachedOrRemoteClientID() = api.fetchCachedOrRemoteClientID()

    fun sendAnalyticsEvent(name: String) {
        api.sendAnalyticsEvent(name)
    }
}
