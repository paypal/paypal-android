package com.paypal.android.card

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.paypal.android.card.api.CardAPI
import com.paypal.android.card.api.ConfirmPaymentSourceResponse
import com.paypal.android.card.model.CardResult
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24")
    private val orderID = "sample-order-id"

    private val cardAPI = mockk<CardAPI>(relaxed = true)
    private val confirmPaymentSourceResponse =
        ConfirmPaymentSourceResponse("orderId", OrderStatus.APPROVED)

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val browserSwitchClient = mockk<BrowserSwitchClient>(relaxed = true)

    private val approveOrderListener = mockk<ApproveOrderListener>(relaxed = true)

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    private lateinit var sut: CardClient

    @Before
    fun beforeEach() {
        sut = CardClient(activity, cardAPI, browserSwitchClient)
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `approve order confirms payment source using card api`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val request = CardRequest(orderID, card)
        coEvery { cardAPI.confirmPaymentSource(request) } returns confirmPaymentSourceResponse

        sut.approveOrderListener = approveOrderListener
        sut.approveOrder(activity, request, dispatcher)
        advanceUntilIdle()

        val resultSlot = slot<CardResult>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("sample-order-id", actual.orderID)
    }

    @Test
    fun `approve order throws paypal error`() {
        val errorMessage = "mock_error_message"
        val request = CardRequest(orderID, card)
        coEvery { cardAPI.confirmPaymentSource(request) } throws (PayPalSDKError(
            0,
            errorMessage
        ))

        val exception =
            assertThrows(PayPalSDKError::class.java) { runBlocking { sut.approveOrder(
                activity,
                request
            ) } }
        assert(exception.errorDescription == errorMessage)
    }

    @Test
    fun `approve order with callback confirms payment source using card api`() {
        val request = CardRequest(orderID, card)
        val resultSlot = slot<CardResult>()

        coEvery { cardAPI.confirmPaymentSource(request) } returns confirmPaymentSourceResponse

        sut.approveOrderListener = approveOrderListener
        sut.approveOrder(activity, request)

        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(resultSlot)) }
        assertSame(resultSlot.captured, confirmPaymentSourceResponse)
    }

    @Test
    fun `approve order with callback confirms throws error`() {
        val errorMessage = "mock_error_message"
        val request = CardRequest(orderID, card)
        val approveOrderCallbackMock = mockk<ApproveOrderListener>(relaxed = true)
        val exceptionSlot = slot<PayPalSDKError>()

        coEvery { cardAPI.confirmPaymentSource(request) } throws (PayPalSDKError(
            0,
            errorMessage
        ))

        sut.approveOrderListener = approveOrderCallbackMock
        sut.approveOrder(activity, request)

        verify(exactly = 1) { approveOrderCallbackMock.onApproveOrderFailure(capture(exceptionSlot)) }
        assert(exceptionSlot.captured.errorDescription == errorMessage)
    }
}
