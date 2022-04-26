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
}