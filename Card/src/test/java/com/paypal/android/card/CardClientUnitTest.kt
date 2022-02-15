package com.paypal.android.card

import com.paypal.android.core.OrderStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
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
        coEvery { cardAPI.confirmPaymentSource(orderID, card) } returns cardResult
    }

    @Test
    fun `approve order confirms payment source using card api`() = runBlockingTest {
        val request = CardRequest(orderID, card)

        val actualResult = sut.approveOrder(request)
        assertSame(actualResult, cardResult)
    }
}
