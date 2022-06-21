package com.paypal.android.card

import com.paypal.android.card.threedsecure.SCA
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.core.Address
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class CardRequestFactoryUnitTest {

    private val orderID = "sample-order-id"

    private lateinit var sut: CardRequestFactory

    @Before
    fun beforeEach() {
        sut = CardRequestFactory()
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

        val cardRequest = CardRequest(orderID, card)
        val apiRequest = sut.createConfirmPaymentSourceRequest(cardRequest)
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
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(apiRequest.body!!), false)
    }

    @Test
    fun `it omits optional params when they are not set`() {
        val card =
            Card(number = "4111111111111111", expirationMonth = "01", expirationYear = "2022")

        val cardRequest = CardRequest(orderID, card)
        val apiRequest = sut.createConfirmPaymentSourceRequest(cardRequest)
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
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(apiRequest.body!!), false)
    }

    @Test
    fun `it optionally request 3DS strong consumer authentication`() {
        val card =
            Card(number = "4111111111111111", expirationMonth = "01", expirationYear = "2022")

        val cardRequest = CardRequest(orderID, card)
        cardRequest.threeDSecureRequest = ThreeDSecureRequest(
            SCA.SCA_ALWAYS,
            "https://sample.com/return/url",
            "https://sample.com/return/url"
        )

        val apiRequest = sut.createConfirmPaymentSourceRequest(cardRequest)
        assertEquals("v2/checkout/orders/sample-order-id/confirm-payment-source", apiRequest.path)

        // language=JSON
        val expectedJSON = """
            {
              "payment_source": {
                "card": {
                  "number": "4111111111111111",
                  "expiry": "2022-01",
                  "attributes": {
                    "verification": {
                      "method": "SCA_ALWAYS"
                    }
                  }
                }
              }
            }
        """.trimIndent()
        JSONAssert.assertEquals(JSONObject(expectedJSON), JSONObject(apiRequest.body!!), false)
    }
}
