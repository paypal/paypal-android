package com.paypal.android.cardpayments

import android.app.Application
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.CheckoutOrdersAPI
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.cardpayments.model.PaymentSource
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderId = "sample-order-id"

    private val cardRequest = CardRequest(orderId, card, "return_url")
    private val cardVaultRequest = CardVaultRequest(setupTokenId = "fake-setup-token-id", card = card)

    private val checkoutOrdersAPI = mockk<CheckoutOrdersAPI>(relaxed = true)
    private val paymentMethodTokensAPI = mockk<DataVaultPaymentMethodTokensAPI>(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val confirmPaymentSourceResponse =
        ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED)

    private val paymentSource = PaymentSource("1111", "Visa")
    private val approveOrderMetadata = ApproveOrderMetadata("sample-order-id", paymentSource)

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val activityLifecycle = mockk<Lifecycle>(relaxed = true)

    private val browserSwitchClient = mockk<BrowserSwitchClient>(relaxed = true)

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
    fun `approve order performs browser switch when payer action is required`() = runTest {
        val sut = createCardClient(testScheduler)

        val threeDSecureAuthChallengeResponse =
            ConfirmPaymentSourceResponse(orderId, OrderStatus.APPROVED, "/payer/action/href")

        coEvery { checkoutOrdersAPI.confirmPaymentSource(cardRequest) } returns threeDSecureAuthChallengeResponse

        sut.approveOrder(activity, cardRequest)
        advanceUntilIdle()

        val browserSwitchOptionsSlot = slot<BrowserSwitchOptions>()
        verify(exactly = 1) {
            browserSwitchClient.start(
                activity,
                capture(browserSwitchOptionsSlot)
            )
        }

        val browserSwitchOptions = browserSwitchOptionsSlot.captured
        assertEquals(Uri.parse("/payer/action/href"), browserSwitchOptions.url)
    }

    @Test
    fun `handle browser switch result notifies user of success with updated order info`() =
        runTest {
            val sut = createCardClient(testScheduler)

            val browserSwitchResult =
                createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, approveOrderMetadata)
            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

            sut.handleBrowserSwitchResult(activity)
            advanceUntilIdle()

            val cardResultSlot = slot<CardResult>()
            coVerify(exactly = 1) {
                approveOrderListener.onApproveOrderSuccess(capture(cardResultSlot))
            }

            val cardResult = cardResultSlot.captured
            assertEquals("sample-order-id", cardResult.orderId)
        }

    @Test
    fun `handle browser switch result notifies listener of cancelation`() = runTest {
        val sut = createCardClient(testScheduler)

        val browserSwitchResult = createBrowserSwitchResult(BrowserSwitchStatus.CANCELED)
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        sut.handleBrowserSwitchResult(activity)
        verify(exactly = 1) { approveOrderListener.onApproveOrderCanceled() }
    }

    @Test
    fun `vault notifies listener of update setup token success`() = runTest {
        val sut = createCardClient(testScheduler)

        val cardVaultResult = CardVaultResult("fake-setup-token-id-from-result", "fake-status")
        coEvery {
            paymentMethodTokensAPI.updateSetupToken(applicationContext, "fake-setup-token-id", card)
        } returns cardVaultResult

        sut.vault(activity, cardVaultRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardVaultResult>()
        verify(exactly = 1) { cardVaultListener.onVaultSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals(cardVaultResult, actual)
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

    private fun createCardClient(testScheduler: TestCoroutineScheduler): CardClient {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sut = CardClient(
            activity,
            checkoutOrdersAPI,
            paymentMethodTokensAPI,
            analyticsService,
            browserSwitchClient,
            dispatcher
        )
        sut.approveOrderListener = approveOrderListener
        sut.cardVaultListener = cardVaultListener
        return sut
    }

    private fun createBrowserSwitchResult(
        @BrowserSwitchStatus status: Int,
        metadata: ApproveOrderMetadata? = null
    ): BrowserSwitchResult {

        val browserSwitchResult = mockk<BrowserSwitchResult>(relaxed = true)
        every { browserSwitchResult.status } returns status

        every { browserSwitchResult.requestMetadata } returns metadata?.toJSON()
        return browserSwitchResult
    }
}
