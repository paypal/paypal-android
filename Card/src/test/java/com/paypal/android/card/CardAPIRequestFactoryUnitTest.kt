package com.paypal.android.card

import com.paypal.android.core.Address
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class CardAPIRequestFactoryUnitTest {

    private val orderID = "sample-order-id"

    private lateinit var sut: CardAPIRequestFactory

    @Before
    fun beforeEach() {
        sut = CardAPIRequestFactory()
    }

    @Test
    fun `it builds a confirm payment source request`() {
        val card = Card(
            cardholderName = "Cardholder Name",
            number = "4111111111111111",
            expirationMonth = "01",
            expirationYear = "2022",
            securityCode = "123",
            billingAddress = Address(
                streetAddress = "2211 N 1st St.",
                extendedAddress = "Apt. 1A",
                locality = "San Jose",
                region = "CA",
                postalCode = "95131",
                countryCode = "US"
            )
        )

        val apiRequest = sut.createConfirmPaymentSourceRequest(orderID, card)
        assertEquals("v2/checkout/orders/sample-order-id/confirm-payment-source", apiRequest.path)

        // language=JSON
        val expectedJSON = """
            {
              "payment_source": {
                "card": {
                  "name": "Cardholder Name",
                  "number": "4111111111111111",
                  "expiry": "2022-01",
                  "security_code": "123",
                  "billing_address": {
                    "address_line_1": "2211 N 1st St.",
                    "address_line_2": "Apt. 1A",
                    "admin_area_1": "CA",
                    "admin_area_2": "San Jose",
                    "postal_code": "95131",
                    "country_code": "US"
                  }
                }
              }
            }
        """.trimIndent()
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(apiRequest.body!!), true)
    }

    @Test
    fun `it omits optional params when they are not set`() {
        val card = Card(number = "4111111111111111", expirationMonth = "01", expirationYear = "2022")

        val apiRequest = sut.createConfirmPaymentSourceRequest(orderID, card)
        assertEquals("v2/checkout/orders/sample-order-id/confirm-payment-source", apiRequest.path)

        // language=JSON
        val expectedJSON = """
            {
              "payment_source": {
                "card": {
                  "number": "4111111111111111",
                  "expiry": "2022-01"
                }
              }
            }
        """.trimIndent()
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(apiRequest.body!!), true)
    }
}
