package com.paypal.android.card

import com.paypal.android.core.API
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.OrderStatus
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardAPIUnitTest {

    private val api = mockk<API>(relaxed = true)
    private val requestBuilder = mockk<CardAPIRequestFactory>()

    private val card = Card("4111111111111111", "01", "24")
    private val orderID = "sample-order-id"
    private val apiRequest = APIRequest("/sample/path", HttpMethod.POST, null)

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
        val httpResponse = HttpResponse(200, body)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)

        val confirmedPaymentSource = result.response
        assertEquals("testOrderID", confirmedPaymentSource?.orderID)
        assertEquals(OrderStatus.APPROVED, confirmedPaymentSource?.status)
    }

    @Test
    fun `it returns an error when the order is not found`() = runBlockingTest {
        // language=JSON
        val body = """
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
        val httpResponse = HttpResponse(404, body)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)
        assertNull(result.response)
        assertEquals("RESOURCE_NOT_FOUND", result.error!!.name)
        assertEquals("The specified resource does not exist.", result.error!!.message)

        val firstErrorDetail = result.error!!.details.first()
        assertEquals("INVALID_RESOURCE_ID", firstErrorDetail.issue)
        assertEquals("Specified resource ID does not exist.", firstErrorDetail.description)
    }

    @Test
    fun `it returns an error when the response is malformed`() = runBlockingTest {
        // language=JSON
        val body = """
            {
                "some_unexpected_response": "something"
            }
        """
        val httpResponse = HttpResponse(200, body)
        coEvery { api.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)
        assertNull(result.response)
        assertEquals("PARSING_ERROR", result.error!!.name)
        assertEquals("Error parsing json response.", result.error!!.message)
    }
}
