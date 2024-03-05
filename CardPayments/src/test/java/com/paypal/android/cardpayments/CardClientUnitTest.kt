package com.paypal.android.cardpayments

import android.app.Application
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderId = "sample-order-id"

    private val cardRequest = CardRequest(orderId, card, "merchant.app://return_url")
    private val cardVaultRequest =
        CardVaultRequest(setupTokenId = "fake-setup-token-id", card = card)

    private val cardAuthLauncher = mockk<CardAuthLauncher>(relaxed = true)

    private val checkoutOrdersAPI = mockk<CheckoutOrdersAPI>(relaxed = true)
    private val paymentMethodTokensAPI = mockk<DataVaultPaymentMethodTokensAPI>(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val confirmPaymentSourceResponse =
        ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED)

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val activityLifecycle = mockk<Lifecycle>(relaxed = true)

    private val approveOrderListener = mockk<ApproveOrderListener>(relaxed = true)
    private val cardVaultListener = mockk<CardVaultListener>(relaxed = true)

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

        every { activity.applicationContext } returns applicationContext
        every { activity.lifecycle } returns activityLifecycle
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `register lifecycle observer on init`() = runTest {
        createCardClient(testScheduler)
        verify(exactly = 1) { activityLifecycle.addObserver(any<CardLifeCycleObserver>()) }
    }

    @Test
    fun `approve order notifies listener of confirm payment source success`() = runTest {
        val sut = createCardClient(testScheduler)

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns confirmPaymentSourceResponse

        sut.approveOrder(activity, cardRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardResult>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("sample-order-id", actual.orderId)
    }

    @Test
    fun `approve order notifies listener of confirm payment source failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } throws error

        sut.approveOrder(activity, cardRequest)
        advanceUntilIdle()

        val errorSlot = slot<PayPalSDKError>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderFailure(capture(errorSlot)) }

        val capturedError = errorSlot.captured
        assertEquals("mock_error_message", capturedError.errorDescription)
    }

    @Test
    fun `approveOrder() presents auth challenge when payer action is required`() = runTest {
        val threeDSecureAuthChallengeResponse =
            ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED, "/payer/action/href")

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns threeDSecureAuthChallengeResponse

        val sut = createCardClient(testScheduler)
        sut.approveOrder(activity, cardRequest)
        advanceUntilIdle()

        val authChallengeSlot = slot<CardAuthChallenge>()
        verify { cardAuthLauncher.presentAuthChallenge(activity, capture(authChallengeSlot)) }

        val authChallenge = authChallengeSlot.captured as CardAuthChallenge.ApproveOrder
        assertEquals(Uri.parse("/payer/action/href"), authChallenge.url)
        assertEquals(cardRequest, authChallenge.request)
        assertEquals("merchant.app", authChallenge.returnUrlScheme)
    }

    @Test
    fun `vault notifies listener of update setup token success`() = runTest {
        val sut = createCardClient(testScheduler)

        val updateSetupTokenResult =
            UpdateSetupTokenResult("fake-setup-token-id-from-result", "fake-status", null)
        coEvery {
            paymentMethodTokensAPI.updateSetupToken(applicationContext, "fake-setup-token-id", card)
        } returns updateSetupTokenResult

        sut.vault(activity, cardVaultRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { cardVaultListener.onVaultSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("fake-setup-token-id-from-result", actual.setupTokenId)
        assertEquals("fake-status", actual.status)
        assertNull(actual.authChallenge)
    }

    @Test
    fun `vault notifies listener of update setup token failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery {
            paymentMethodTokensAPI.updateSetupToken(applicationContext, "fake-setup-token-id", card)
        } throws error

        sut.vault(activity, cardVaultRequest)
        advanceUntilIdle()

        val errorSlot = slot<PayPalSDKError>()
        verify(exactly = 1) { cardVaultListener.onVaultFailure(capture(errorSlot)) }

        val capturedError = errorSlot.captured
        assertEquals("mock_error_message", capturedError.errorDescription)
    }

    @Test
    fun `handleBrowserSwitchResult() notifies merchant of approve order success`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        val successResult = CardResult("fake-order-id")
        every {
            cardAuthLauncher.deliverBrowserSwitchResult(activity)
        } returns CardStatus.ApproveOrderSuccess(successResult)

        sut.handleBrowserSwitchResult(activity)

        val slot = slot<CardResult>()
        verify(exactly = 1) { sut.approveOrderListener?.onApproveOrderSuccess(capture(slot)) }
        assertSame(successResult, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult() notifies merchant of approve order failure`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            cardAuthLauncher.deliverBrowserSwitchResult(activity)
        } returns CardStatus.ApproveOrderError(error)

        sut.handleBrowserSwitchResult(activity)

        val slot = slot<PayPalSDKError>()
        verify(exactly = 1) { sut.approveOrderListener?.onApproveOrderFailure(capture(slot)) }
        assertSame(error, slot.captured)
    }

    @Test
    fun `handleBrowserSwitchResult() notifies merchant of approve order cancelation`() = runTest {
        val sut = createCardClient(testScheduler)
        sut.approveOrderListener = approveOrderListener

        every {
            cardAuthLauncher.deliverBrowserSwitchResult(activity)
        } returns CardStatus.ApproveOrderCanceled("fake-order-id")

        sut.handleBrowserSwitchResult(activity)
        verify(exactly = 1) { sut.approveOrderListener?.onApproveOrderCanceled() }
    }

    @Test
    fun `handleBrowserSwitchResult() doesn't deliver result when browserSwitchResult is null`() =
        runTest {
            val sut = createCardClient(testScheduler)
            sut.approveOrderListener = approveOrderListener
            sut.cardVaultListener = cardVaultListener

            every { cardAuthLauncher.deliverBrowserSwitchResult(activity) } returns null

            sut.handleBrowserSwitchResult(activity)
            verify { sut.approveOrderListener?.wasNot(Called) }
            verify { sut.cardVaultListener?.wasNot(Called) }
        }

    private fun createCardClient(testScheduler: TestCoroutineScheduler): CardClient {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sut = CardClient(
            activity,
            checkoutOrdersAPI,
            paymentMethodTokensAPI,
            analyticsService,
            cardAuthLauncher,
            dispatcher
        )
        sut.approveOrderListener = approveOrderListener
        sut.cardVaultListener = cardVaultListener
        return sut
    }
}
