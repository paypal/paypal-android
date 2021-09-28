package com.paypal.android.card

import com.paypal.android.core.APIClient
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.HttpResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardAPIClientUnitTest {

    private val apiClient = mockk<APIClient>(relaxed = true)
    private val requestBuilder = mockk<CardAPIRequestBuilder>()

    private val card = Card()
    private val orderID = "sample-order-id"
    private val apiRequest = APIRequest("/sample/path", HttpMethod.POST, null)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: CardAPIClient

    @Before
    fun beforeEach() {
        sut = CardAPIClient(apiClient, requestBuilder)
        every { requestBuilder.buildConfirmPaymentSourceRequest(orderID, card) } returns apiRequest

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `it sends a confirm payment source api request`() = runBlockingTest {
        sut.confirmPaymentSource(orderID, card)
        coVerify { apiClient.send(apiRequest) }
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
        coEvery { apiClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)

        val confirmedPaymentSource = result.response
        assertEquals("testOrderID", confirmedPaymentSource?.orderID)
        assertEquals("APPROVED", confirmedPaymentSource?.status)
        assertEquals("7321", confirmedPaymentSource?.lastDigits)
        assertEquals("VISA", confirmedPaymentSource?.brand)
        assertEquals("CREDIT", confirmedPaymentSource?.type)
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
                        "description": "Specified resource ID does not exist. "
                    }
                ],
                "message": "The specified resource does not exist.",
                "debug_id": "81db5c4ddfa35"
            }
        """
        val httpResponse = HttpResponse(404, body)
        coEvery { apiClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)
        assertNull(result.response)
        assertNotNull(result.error)
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
        coEvery { apiClient.send(apiRequest) } returns httpResponse

        val result = sut.confirmPaymentSource(orderID, card)
        assertNull(result.response)
        assertNotNull(result.error)
    }
}
