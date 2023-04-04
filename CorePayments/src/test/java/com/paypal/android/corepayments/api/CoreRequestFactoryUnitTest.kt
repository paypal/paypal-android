package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.api.models.GetOrderRequest
import org.junit.Test
import org.junit.Assert.assertEquals


class CoreRequestFactoryUnitTest {

    @Test
    fun `it builds a get order info request`() {
        val mockOrderID = "mock_order_ID"
        val sut = CoreRequestFactory()

        val apiRequest = sut.createGetOrderInfoRequest(GetOrderRequest(mockOrderID))

        assertEquals(apiRequest.path, "v2/checkout/orders/$mockOrderID")
        assertEquals(apiRequest.method, HttpMethod.GET)
        assertEquals(apiRequest.body, "")
    }
}