package com.paypal.android.cardpayments

import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.HttpURLConnection

@RunWith(Enclosed::class)
class CardRequestParserUnitTest {

    class CardRequestParserFunctionsUnitTest {

        private val correlationId = "correlation-id"
        private val headers: Map<String, String> = mapOf(
            "Paypal-Debug-Id" to correlationId,
        )

        private lateinit var sut: CardResponseParser

        @Before
        fun setUp() {
            sut = CardResponseParser()
        }

        @Test
        fun `parse error returns null if http response is successful`() {
            val httpResponse = mockk<HttpResponse>(relaxed = true)
            every { httpResponse.isSuccessful } returns true

            Assert.assertNull(sut.parseError(httpResponse))
        }
    }

    @RunWith(Parameterized::class)
    internal class CardRequestParserErrorParameterized(
        private val isSuccessful: Boolean,
        private val status: Int,
        private val body: String,
        private val message: String,
        private val resultCode: PayPalSDKErrorCode
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
                    PayPalSDKErrorCode.UNKNOWN_HOST
                ),
                arrayOf(
                    false,
                    HttpResponse.STATUS_UNDETERMINED,
                    "json_body",
                    "An unknown error occurred. Contact developer.paypal.com/support.",
                    PayPalSDKErrorCode.UNKNOWN
                ),
                arrayOf(
                    false,
                    HttpResponse.SERVER_ERROR,
                    "json_body",
                    "A server error occurred. Contact developer.paypal.com/support.",
                    PayPalSDKErrorCode.SERVER_RESPONSE_ERROR
                ),
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
                    PayPalSDKErrorCode.CHECKOUT_ERROR
                )
            )
        }

        private val correlationId = "correlation-id"
        private val headers: Map<String, String> = mapOf(
            "Paypal-Debug-Id" to correlationId,
        )

        @Test
        fun `parse error with `() {
            val httpResponse = mockk<HttpResponse>(relaxed = true)

            every { httpResponse.isSuccessful } returns isSuccessful
            every { httpResponse.headers } returns headers
            every { httpResponse.status } returns status
            every { httpResponse.body } returns body

            val sut = CardResponseParser()
            val error = sut.parseError(httpResponse)

            if (status != HttpURLConnection.HTTP_OK) {
                Assert.assertEquals(
                    resultCode.ordinal,
                    error?.code
                )
            }
            Assert.assertEquals(correlationId, error?.correlationId)
            Assert.assertEquals(message, error?.errorDescription)
        }
    }
}
