package com.paypal.android.paypalwebpayments

import android.net.Uri
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutDeepLinkResultUnitTest {

    @Test
    fun `given an Uri and metadata, checkoutWebResult should contain the same fields`() {
        val mockPayerId = "fake_payer_id"
        val mockIntent = "fake_intent"
        val mockOpType = "fake_op_type"
        val mockToken = "fake_token"
        val mockOrderId = "fake_order_id"

        val url = "http://testurl.com/checkout?" +
                "PayerID=$mockPayerId" +
                "&intent=$mockIntent" +
                "&opType=$mockOpType" +
                "&token=$mockToken"

        val metadata = JSONObject()
        metadata.put("order_id", mockOrderId)

        val webResult = PayPalDeepLinkUrlResult(Uri.parse(url), metadata)
        expectThat(webResult) {
            get { payerId }.isEqualTo(mockPayerId)
            get { intent }.isEqualTo(mockIntent)
            get { opType }.isEqualTo(mockOpType)
            get { token }.isEqualTo(mockToken)
            get { orderId }.isEqualTo(mockOrderId)
        }
    }
}
