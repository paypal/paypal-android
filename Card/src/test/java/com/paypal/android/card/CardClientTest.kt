package com.paypal.android.card

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardClientTest {

    private val card = Card()
    private val orderID = "sample-order-id"
    private val confirmPaymentSourceResult = ConfirmPaymentSourceResult()

    private val cardAPIClient = mockk<CardAPIClient>(relaxed = true)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private lateinit var sut: CardClient

    @Before
    fun beforeEach() {
        sut = CardClient(cardAPIClient, testCoroutineDispatcher)

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `it forwards result from confirm payment source api via completion handler`() {
        coEvery {
            cardAPIClient.confirmPaymentSource(orderID, card)
        } returns confirmPaymentSourceResult

        lateinit var capturedResult: ConfirmPaymentSourceResult
        sut.confirmPaymentSource(orderID, card) { capturedResult = it }

        assertSame(capturedResult, confirmPaymentSourceResult)
    }
}