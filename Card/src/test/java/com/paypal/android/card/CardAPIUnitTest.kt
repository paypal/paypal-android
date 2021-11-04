package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.OrderStatus
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

    private val api = mockk<API>(relaxed = true)
    private val requestBuilder = mockk<CardAPIRequestFactory>()
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

    private lateinit var sut: CardAPI

    @Before
    fun beforeEach() {
        sut = CardAPI(api, requestBuilder)
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
        val httpResponse = HttpResponse(200)
        coEvery { api.send(apiRequest) } returns httpResponse

        sut.confirmPaymentSource(orderID, card)
        coVerify { api.send(apiRequest) }
    }

    @Test
    fun `it returns a confirm payment source result`() = runBlockingTest {
        // language=JSON
        val body = """
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
        val httpResponse = HttpResponse(200, headers, body)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Success

        assertEquals("testOrderID", result.orderID)
        assertEquals(OrderStatus.APPROVED, result.status)
    }

    @Test
    fun `it returns an error when the order is not found`() = runBlockingTest {
        val httpResponse = HttpResponse(404, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            "The specified resource does not exist. -> [Issue: INVALID_RESOURCE_ID.\n" +
                    "Error description: Specified resource ID does not exist.]",
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `it returns unknownError when the order api call returns an error body`() = runBlockingTest {
        // Status: STATUS_UNDETERMINED
        val httpResponse = HttpResponse(-1, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            APIClientError.unknownError.errorDescription,
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `it returns noResponseData when the order api call returns an empty body`() = runBlockingTest {
        // Status: ANY
        val httpResponse = HttpResponse(-10, headers, emptyErrorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            APIClientError.noResponseData.errorDescription,
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `it returns dataParsingError when the order api call returns an error body`() = runBlockingTest {
        // Status: OK
        val httpResponse = HttpResponse(200, headers, errorBody)
        val parsingException = JSONException("Parsing Error")
        coEvery { api.send(apiRequest) } returns httpResponse
        every { paymentsJSON.getString(any()) } throws parsingException

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            APIClientError.dataParsingError.errorDescription,
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `it returns unknownHost when the order api call returns an error body`() = runBlockingTest {
        // Status: STATUS_UNKNOWN_HOST
        val httpResponse = HttpResponse(-2, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            APIClientError.unknownHost.errorDescription,
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `it returns serverError when the order api call returns an error body`() = runBlockingTest {
        // Status: SERVER_ERROR
        val httpResponse = HttpResponse(-3, headers, errorBody)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error
        assertEquals(
            APIClientError.serverResponseError.errorDescription,
            result.payPalSDKError.errorDescription
        )
    }

    @Test
    fun `when confirmPaymentSource fails to parse response, correlation ID is set in Error`() =
        runBlockingTest {
            coEvery { api.send(apiRequest) } returns HttpResponse(200, headers, unexpectedBody)

            val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error

            assertEquals(correlationId, result.correlationID)
        }

    @Test
    fun `when confirmPaymentSource is errors, correlation ID is set in Error`() = runBlockingTest {
        coEvery { api.send(apiRequest) } returns HttpResponse(400, headers, errorBody)

        val result = sut.confirmPaymentSource(orderID, card) as CardResult.Error

        assertEquals(correlationId, result.correlationID)
    }
}
