package com.paypal.android.checkout

import android.net.Uri
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
class PayPalCheckoutWebResultUnitTest {

    @Test
    fun `given an Uri and metadata, checkoutWebResult should contain the same fields`() {
        val mockPayerId = generateRandomString()
        val mockIntent = generateRandomString()
        val mockOpType = generateRandomString()
        val mockToken = generateRandomString()
        val mockOrderId = generateRandomString();

        val url = "http://testurl.com/checkout?PayerID=$mockPayerId&intent=$mockIntent&opType=$mockOpType&token=$mockToken"

        val metadata = JSONObject()
        metadata.put("order_id", mockOrderId)


        val webResult = PayPalCheckoutWebResult(Uri.parse(url), metadata)
        expectThat(webResult) {
            get { payerId }.isEqualTo(mockPayerId)
            get { intent }.isEqualTo(mockIntent)
            get { opType }.isEqualTo(mockOpType)
            get { token }.isEqualTo(mockToken)
            get { orderId }.isEqualTo(mockOrderId)
        }
    }
}