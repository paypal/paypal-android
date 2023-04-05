package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.Code
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.api.models.GetOrderInfoResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.HttpURLConnection

@RunWith(Enclosed::class)
class CoreRequestParserUnitTest {

    class CoreRequestParserFunctionsUnitTest {

        private val correlationID = "correlationID"
        private val headers: Map<String, String> = mapOf(
            "Paypal-Debug-Id" to correlationID,
        )

        @Test
        fun `it parses an get info order response`() {
            val response = """ 
            {
                "id":"89H32494DY5382259",
                "intent":"AUTHORIZE",
                "status":"CREATED",
                "payment_source":{
                    "card":{
                        "last_digits":"2661",
                        "brand":"VISA",
                        "type":"CREDIT",
                        "authentication_result":{
                            "liability_shift":"NO",
                            "three_d_secure":{
                                "enrollment_status":"U"
                            }
                        }
                    }
                },
                "purchase_units":[
                    {
                        "reference_id":"default",
                        "amount":{
                            "currency_code":"USD",
                            "value":"10.99"
                        },
                        "payee":{
                            "email_address":"sb-nsaqd6969163@business.example.com",
                            "merchant_id":"87YQEA4JG8AUW"
                        }
                    }
                ]
                ]
            }
        """.trimIndent()

            val mockHttpResponse = mockk<HttpResponse>(relaxed = true)
            every { mockHttpResponse.body } returns response

            val sut = CoreRequestParser()

            val expected = GetOrderInfoResponse(PaymentsJSON(response))
            val actual = sut.parseGetOrderInfoResponse(mockHttpResponse)

            Assert.assertEquals(expected, actual)
        }

        @Test
        fun `it throws an error if json is invalid`() {
            val invalidJsonResponse = "invalid_json"
            val mockHttpResponse = mockk<HttpResponse>(relaxed = true)
            every { mockHttpResponse.body } returns invalidJsonResponse
            every { mockHttpResponse.headers } returns headers

            val sut = CoreRequestParser()

            var capturedError: PayPalSDKError? = null
            try {
                sut.parseGetOrderInfoResponse(mockHttpResponse)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            Assert.assertEquals(Code.DATA_PARSING_ERROR.ordinal, capturedError?.code)
            Assert.assertEquals(correlationID, capturedError?.correlationID)
        }

        @Test
        fun `parse error returns null if http response is successful`() {
            val httpResponse = mockk<HttpResponse>(relaxed = true)
            every { httpResponse.isSuccessful } returns true

            val sut = CoreRequestParser()
            Assert.assertNull(sut.parseError(httpResponse))
        }
    }

    @RunWith(Parameterized::class)
    internal class CoreRequestParserErrorParameterized(
        private val isSuccessful: Boolean,
        private val status: Int,
        private val body: String,
        private val message: String,
        private val resultCode: Code
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "http response status code: {1}, resulting in code: {4}")
            fun responseScenarios() = listOf(
                arrayOf(
                    false,
                    HttpResponse.STATUS_UNKNOWN_HOST,
                    "json_body",
                    "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
                    Code.UNKNOWN_HOST),
                arrayOf(
                    false,
                    HttpResponse.STATUS_UNDETERMINED,
                    "json_body",
                    "An unknown error occurred. Contact developer.paypal.com/support.",
                    Code.UNKNOWN),
                arrayOf(
                    false,
                    HttpResponse.SERVER_ERROR,
                    "json_body",
                    "A server error occurred. Contact developer.paypal.com/support.",
                    Code.SERVER_RESPONSE_ERROR),
                arrayOf(
                    false,
                    HttpURLConnection.HTTP_OK,
                    """
                       {
                          "message": "error message",
                          "debug_id": "cab9ca88238f8",
                          "details": [
                            {
                              "issue": "ISSUE_KEY_TITLE",
                              "description": "issue description message"
                            }
                          ]
                        }
                    """.trimIndent(),
                    "error message -> [Issue: ISSUE_KEY_TITLE.\n" +
                            "Error description: issue description message]",
                    Code.CHECKOUT_ERROR)
            )
        }

        private val correlationID = "correlationID"
        private val headers: Map<String, String> = mapOf(
            "Paypal-Debug-Id" to correlationID,
        )

        @Test
        fun `parse error with `() {
            val httpResponse = mockk<HttpResponse>(relaxed = true)

            every { httpResponse.isSuccessful } returns isSuccessful
            every { httpResponse.headers } returns headers
            every { httpResponse.status } returns status
            every { httpResponse.body } returns body

            val sut = CoreRequestParser()
            val error = sut.parseError(httpResponse)

            if (status != HttpURLConnection.HTTP_OK) Assert.assertEquals(resultCode.ordinal, error?.code)
            Assert.assertEquals(correlationID, error?.correlationID)
            Assert.assertEquals(message, error?.errorDescription)
        }
    }
}
