package com.paypal.android.card

import com.paypal.android.card.api.CardAPI
import com.paypal.android.card.model.CardResult
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24")
    private val orderID = "sample-order-id"

    private val cardAPI = mockk<CardAPI>(relaxed = true)
    private val cardResult = CardResult("orderId", OrderStatus.APPROVED)

    private lateinit var sut: CardClient

    @Before
    fun beforeEach() {
        sut = CardClient(cardAPI)
    }

    @Test
    fun `approve order confirms payment source using card api`() = runBlockingTest {
        val request = CardRequest(orderID, card)
        coEvery { cardAPI.confirmPaymentSource(orderID, card) } returns cardResult

        val actualResult = sut.approveOrder(request)
        assertSame(actualResult, cardResult)
    }

    @Test
    fun `approve order throws paypal error`() {
        val errorMessage = "mock_error_message"
        val request = CardRequest(orderID, card)
        coEvery { cardAPI.confirmPaymentSource(orderID, card) } throws(PayPalSDKError(0, errorMessage))

        val exception = assertThrows(PayPalSDKError::class.java) { runBlocking { sut.approveOrder(request) } }
        assert(exception.errorDescription == errorMessage)
    }

    @Test
    fun `approve order with callback confirms payment source using card api`() {
        val request = CardRequest(orderID, card)
        val approveOrderCallbackMock = mockk<ApproveOrderListener>(relaxed = true)
        val resultSlot = slot<CardResult>()

        coEvery { cardAPI.confirmPaymentSource(orderID, card) } returns cardResult

        sut.approveOrder(request, approveOrderCallbackMock)

        verify(exactly = 1) { approveOrderCallbackMock.success(capture(resultSlot)) }
        assertSame(resultSlot.captured, cardResult)
    }

    @Test
    fun `approve order with callback confirms throws error`() {
        val errorMessage = "mock_error_message"
        val request = CardRequest(orderID, card)
        val approveOrderCallbackMock = mockk<ApproveOrderListener>(relaxed = true)
        val exceptionSlot = slot<PayPalSDKError>()

        coEvery { cardAPI.confirmPaymentSource(orderID, card) } throws(PayPalSDKError(0, errorMessage))

        sut.approveOrder(request, approveOrderCallbackMock)

        verify(exactly = 1) { approveOrderCallbackMock.failure(capture(exceptionSlot)) }
        assert(exceptionSlot.captured.errorDescription == errorMessage)
    }
}
