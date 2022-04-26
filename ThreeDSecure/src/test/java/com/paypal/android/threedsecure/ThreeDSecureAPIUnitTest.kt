package com.paypal.android.threedsecure

import com.paypal.android.card.Card
import com.paypal.android.core.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ThreeDSecureAPIUnitTest {

    // language=JSON
    private val payerActionRequiredBody = """
        {
          "id": "payer-action-order-id",
          "status": "PAYER_ACTION_REQUIRED",
          "purchase_units": [
            {
              "reference_id": "default"
            }
          ],
          "links": [
            {
              "href": "https://www.example.com/order/self/link",
              "rel": "self",
              "method": "GET"
            },
            {
              "href": "https://www.example.com/order/payer-action/link",
              "rel": "payer-action",
              "method": "GET"
            }
          ]
        }
    """.trimIndent()

    // language=JSON
    private val errorBody = """
        {
          "name": "RESOURCE_NOT_FOUND",
          "details": [
            {
              "issue": "SAMPLE_ORDER_ISSUE",
              "description": "Sample Error Description."
            }
          ],
          "message": "Sample Error Message.",
          "debug_id": "81db5c4ddfa35"
        }
    """.trimIndent()

    // language=JSON
    private val unexpectedBody = """
        {
          "some_unexpected_response": "something"
        }
    """.trimIndent()

    private val api = mockk<API>(relaxed = true)
    private val requestBuilder = mockk<ThreeDSecureAPIRequestFactory>()

    private val card = Card("4111111111111111", "01", "24")
    private val orderID = "sample-order-id"
    private val apiRequest = APIRequest("/sample/path", HttpMethod.POST, null)

    private val correlationId = "sample-correlation-id"
    private val headers = mapOf("Paypal-Debug-Id" to correlationId)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: ThreeDSecureAPI

    @Before
    fun beforeEach() {
        sut = ThreeDSecureAPI(api, requestBuilder)
        every { requestBuilder.createConfirmPaymentSourceRequest(orderID, card) } returns apiRequest

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `it sends a confirm payment source api request`() = runBlockingTest {
        val httpResponse = HttpResponse(200, emptyMap(), payerActionRequiredBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        sut.verifyCard(orderID, card)
        coVerify { api.send(apiRequest) }
    }

    @Test
    fun `it returns a 3D secure result`() = runBlockingTest {
        val httpResponse = HttpResponse(200, headers, payerActionRequiredBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.verifyCard(orderID, card)

        assertEquals("payer-action-order-id", result.orderID)
        assertEquals(OrderStatus.PAYER_ACTION_REQUIRED, result.status)
        assertEquals("https://www.example.com/order/payer-action/link", result.payerActionHref)
    }

    @Test
    fun `it returns an error when the order is not found`() = runBlockingTest {
        val httpResponse = HttpResponse(404, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        lateinit var capturedError: PayPalSDKError
        try {
            sut.verifyCard(orderID, card)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(
            "Sample Error Message. -> [Issue: SAMPLE_ORDER_ISSUE.\n" +
                    "Error description: Sample Error Description.]",
            capturedError.errorDescription
        )
    }

    @Test
    fun `it returns noResponseData when the order api call returns an empty body`() =
        runBlockingTest {
            val httpResponse = HttpResponse(123, headers, "")
            coEvery { api.send(apiRequest) } returns httpResponse

            lateinit var capturedError: PayPalSDKError
            try {
                sut.verifyCard(orderID, card)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns an error when the http response contains an undetermined error`() =
        runBlockingTest {
            val httpResponse =
                HttpResponse(HttpResponse.STATUS_UNDETERMINED, headers, errorBody)
            coEvery { api.send(apiRequest) } returns httpResponse

            lateinit var capturedError: PayPalSDKError
            try {
                sut.verifyCard(orderID, card)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An unknown error occurred. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns an error when the http response contains an unknown host error`() =
        runBlockingTest {
            val httpResponse =
                HttpResponse(HttpResponse.STATUS_UNKNOWN_HOST, headers, errorBody)
            coEvery { api.send(apiRequest) } returns httpResponse

            lateinit var capturedError: PayPalSDKError
            try {
                sut.verifyCard(orderID, card)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns an error when the http response contains a server error`() = runBlockingTest {
        val httpResponse = HttpResponse(HttpResponse.SERVER_ERROR, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        lateinit var capturedError: PayPalSDKError
        try {
            sut.verifyCard(orderID, card)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(
            "A server error occurred. Contact developer.paypal.com/support.",
            capturedError.errorDescription
        )
    }

    @Test
    fun `when verify card fails to parse response, correlation ID is set in Error`() =
        runBlockingTest {
            coEvery { api.send(apiRequest) } returns HttpResponse(200, headers, unexpectedBody)

            lateinit var capturedError: PayPalSDKError
            try {
                sut.verifyCard(orderID, card)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(correlationId, capturedError.correlationID)
        }

    @Test
    fun `when verify card is errors, correlation ID is set in Error`() = runBlockingTest {
        coEvery { api.send(apiRequest) } returns HttpResponse(400, headers, errorBody)

        lateinit var capturedError: PayPalSDKError
        try {
            sut.verifyCard(orderID, card)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(correlationId, capturedError.correlationID)
    }
}