package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResult
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.RestClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CheckoutOrdersAPIUnitTest {

    // language=JSON
    private val successBody = """
            {
                "id": "test-order-id",
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

    private val restClient = mockk<RestClient>(relaxed = true)
    private val requestFactory = mockk<CardRequestFactory>()
    private val paymentsJSON = mockk<PaymentsJSON>()

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderId = "sample-order-id"
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

    private val cardRequest = CardRequest(
        orderId,
        card,
        "return_url"
    )

    private lateinit var sut: CheckoutOrdersAPI

    @Before
    fun beforeEach() {
        sut = CheckoutOrdersAPI(restClient, requestFactory)
        every { requestFactory.createConfirmPaymentSourceRequest(cardRequest) } returns apiRequest
    }

    @Test
    fun `it sends a confirm payment source api request`() = runTest {
        val httpResponse = HttpResponse(200, emptyMap(), successBody)
        coEvery { restClient.send(apiRequest) } returns httpResponse

        sut.confirmPaymentSource(cardRequest)
        coVerify { restClient.send(apiRequest) }
    }

    @Test
    fun `it returns a confirm payment source result`() = runTest {
        val httpResponse = HttpResponse(200, headers, successBody)
        coEvery { restClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Success

        assertEquals("test-order-id", result.orderId)
        assertEquals(OrderStatus.APPROVED, result.status)
    }

    @Test
    fun `it returns an error when the order is not found`() = runTest {
        val httpResponse = HttpResponse(404, headers, errorBody)
        coEvery { restClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
        assertEquals(
            "The specified resource does not exist. -> [Issue: INVALID_RESOURCE_ID.\n" +
                    "Error description: Specified resource ID does not exist.]",
            result.error.errorDescription
        )
    }

    @Test
    fun `it returns unknownError when the order api call returns an error body`() =
        runTest {
            // Status: STATUS_UNDETERMINED
            val httpResponse = HttpResponse(-1, headers, errorBody)
            coEvery { restClient.send(apiRequest) } returns httpResponse

            val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
            assertEquals(
                "An unknown error occurred. Contact developer.paypal.com/support.",
                result.error.errorDescription
            )
        }

    @Test
    fun `it returns noResponseData when the order api call returns an empty body`() =
        runTest {
            // Status: ANY
            val httpResponse = HttpResponse(-10, headers, emptyErrorBody)
            coEvery { restClient.send(apiRequest) } returns httpResponse

            val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
            assertEquals(
                "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support.",
                result.error.errorDescription
            )
        }

    @Test
    fun `it returns dataParsingError when the order api call returns an error body`() =
        runTest {
            // Status: OK
            val httpResponse = HttpResponse(200, headers, errorBody)
            val parsingException = JSONException("Parsing Error")
            coEvery { restClient.send(apiRequest) } returns httpResponse
            every { paymentsJSON.getString(any()) } throws parsingException

            val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
            assertEquals(
                "An error occurred parsing HTTP response data. Contact developer.paypal.com/support.",
                result.error.errorDescription
            )
        }

    @Test
    fun `it returns unknownHost when the order api call returns an error body`() = runTest {
        // Status: STATUS_UNKNOWN_HOST
        val httpResponse = HttpResponse(-2, headers, errorBody)
        coEvery { restClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
        assertEquals(
            "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
            result.error.errorDescription
        )
    }

    @Test
    fun `it returns serverError when the order api call returns an error body`() = runTest {
        // Status: SERVER_ERROR
        val httpResponse = HttpResponse(-3, headers, errorBody)
        coEvery { restClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
        assertEquals(
            "A server error occurred. Contact developer.paypal.com/support.",
            result.error.errorDescription
        )
    }

    @Test
    fun `when confirmPaymentSource fails to parse response, correlation ID is set in Error`() =
        runTest {
            coEvery {
                restClient.send(apiRequest)
            } returns HttpResponse(200, headers, unexpectedBody)

            val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
            assertEquals(correlationId, result.error.correlationId)
        }

    @Test
    fun `when confirmPaymentSource is errors, correlation ID is set in Error`() = runTest {
        coEvery { restClient.send(apiRequest) } returns HttpResponse(400, headers, errorBody)

        val result = sut.confirmPaymentSource(cardRequest) as ConfirmPaymentSourceResult.Failure
        assertEquals(correlationId, result.error.correlationId)
    }
}
