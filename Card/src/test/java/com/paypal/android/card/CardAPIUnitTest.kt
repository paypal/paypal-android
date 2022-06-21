package com.paypal.android.card

import com.paypal.android.card.api.CardAPI
import com.paypal.android.core.API
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
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
import org.json.JSONException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardAPIUnitTest {

    // language=JSON
    private val successBody = """
            {
                "id": "testOrderID",
                "status": "APPROVED",
                "payment_source": {
                    "card": {
                        "last_digits": "7321",
                        "brand": "VISA",
                        "type": "CREDIT"
                    }
                }
            }
        """

    private val api = mockk<API>(relaxed = true)
    private val requestFactory = mockk<CardRequestFactory>()
    private val paymentsJSON = mockk<PaymentsJSON>()

    private val card = Card("4111111111111111", "01", "24")
    private val orderID = "sample-order-id"
    private val apiRequest = APIRequest("/sample/path", HttpMethod.POST, null)

    // language=JSON
    private val errorBody = """
            {
                "name": "RESOURCE_NOT_FOUND",
                "details": [
                    {
                        "issue": "INVALID_RESOURCE_ID",
                        "description": "Specified resource ID does not exist."
                    }
                ],
                "message": "The specified resource does not exist.",
                "debug_id": "81db5c4ddfa35"
            }
        """

    // language=JSON
    private val unexpectedBody = """
            {
                "some_unexpected_response": "something"
            }
        """

    private val emptyErrorBody = ""

    private val correlationId = "expected correlation ID"
    private val headers = mapOf("Paypal-Debug-Id" to correlationId)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val cardRequest = CardRequest(orderID, card)

    private lateinit var sut: CardAPI

    @Before
    fun beforeEach() {
        sut = CardAPI(api, requestFactory)
        every { requestFactory.createConfirmPaymentSourceRequest(cardRequest) } returns apiRequest

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `it sends a confirm payment source api request`() = runBlockingTest {
        val httpResponse = HttpResponse(200, emptyMap(), successBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        sut.confirmPaymentSource(cardRequest)
        coVerify { api.send(apiRequest) }
    }

    @Test
    fun `it returns a confirm payment source result`() = runBlockingTest {
        val httpResponse = HttpResponse(200, headers, successBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(cardRequest)

        assertEquals("testOrderID", result.orderID)
        assertEquals(OrderStatus.APPROVED, result.status)
    }

    @Test
    fun `it returns an error when the order is not found`() = runBlockingTest {
        val httpResponse = HttpResponse(404, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        lateinit var capturedError: PayPalSDKError
        try {
            sut.confirmPaymentSource(cardRequest)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(
            "The specified resource does not exist. -> [Issue: INVALID_RESOURCE_ID.\n" +
                    "Error description: Specified resource ID does not exist.]",
            capturedError.errorDescription
        )
    }

    @Test
    fun `it returns unknownError when the order api call returns an error body`() =
        runBlockingTest {
            // Status: STATUS_UNDETERMINED
            val httpResponse = HttpResponse(-1, headers, errorBody)
            coEvery { api.send(apiRequest) } returns httpResponse

            lateinit var capturedError: PayPalSDKError
            try {
                sut.confirmPaymentSource(cardRequest)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An unknown error occurred. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns noResponseData when the order api call returns an empty body`() =
        runBlockingTest {
            // Status: ANY
            val httpResponse = HttpResponse(-10, headers, emptyErrorBody)
            coEvery { api.send(apiRequest) } returns httpResponse

            lateinit var capturedError: PayPalSDKError
            try {
                sut.confirmPaymentSource(cardRequest)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns dataParsingError when the order api call returns an error body`() =
        runBlockingTest {
            // Status: OK
            val httpResponse = HttpResponse(200, headers, errorBody)
            val parsingException = JSONException("Parsing Error")
            coEvery { api.send(apiRequest) } returns httpResponse
            every { paymentsJSON.getString(any()) } throws parsingException

            lateinit var capturedError: PayPalSDKError
            try {
                sut.confirmPaymentSource(cardRequest)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }

            assertEquals(
                "An error occurred parsing HTTP response data. Contact developer.paypal.com/support.",
                capturedError.errorDescription
            )
        }

    @Test
    fun `it returns unknownHost when the order api call returns an error body`() = runBlockingTest {
        // Status: STATUS_UNKNOWN_HOST
        val httpResponse = HttpResponse(-2, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        lateinit var capturedError: PayPalSDKError
        try {
            sut.confirmPaymentSource(cardRequest)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(
            "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
            capturedError.errorDescription
        )
    }

    @Test
    fun `it returns serverError when the order api call returns an error body`() = runBlockingTest {
        // Status: SERVER_ERROR
        val httpResponse = HttpResponse(-3, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        lateinit var capturedError: PayPalSDKError
        try {
            sut.confirmPaymentSource(cardRequest)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(
            "A server error occurred. Contact developer.paypal.com/support.",
            capturedError.errorDescription
        )
    }

    @Test
    fun `when confirmPaymentSource fails to parse response, correlation ID is set in Error`() =
        runBlockingTest {
            coEvery { api.send(apiRequest) } returns HttpResponse(200, headers, unexpectedBody)

            lateinit var capturedError: PayPalSDKError
            try {
                sut.confirmPaymentSource(cardRequest)
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(correlationId, capturedError.correlationID)
        }

    @Test
    fun `when confirmPaymentSource is errors, correlation ID is set in Error`() = runBlockingTest {
        coEvery { api.send(apiRequest) } returns HttpResponse(400, headers, errorBody)

        lateinit var capturedError: PayPalSDKError
        try {
            sut.confirmPaymentSource(cardRequest)
        } catch (e: PayPalSDKError) {
            capturedError = e
        }

        assertEquals(correlationId, capturedError.correlationID)
    }
}
