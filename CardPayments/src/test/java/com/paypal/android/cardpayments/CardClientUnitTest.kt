package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.cardpayments.api.GetOrderInfoResponse
import com.paypal.android.cardpayments.api.GetOrderRequest
import com.paypal.android.cardpayments.api.OrdersAPI
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.SecureTokenServiceAPI
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
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardClientUnitTest {

    private val card = Card("4111111111111111", "01", "24", "123")
    private val orderID = "sample-order-id"

    private val cardRequest = CardRequest(orderID, card, "return_url")

    private val ordersAPI = mockk<OrdersAPI>(relaxed = true)
    private val secureTokenServiceAPI = mockk<SecureTokenServiceAPI>(relaxed = true)
    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val confirmPaymentSourceResponse =
        ConfirmPaymentSourceResponse(orderID, OrderStatus.APPROVED)

    private val paymentSource = PaymentSource("1111", "Visa")
    private val approveOrderMetadata = ApproveOrderMetadata("sample-order-id", paymentSource)

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val activityLifecycle = mockk<Lifecycle>(relaxed = true)

    private val browserSwitchClient = mockk<BrowserSwitchClient>(relaxed = true)

    private val approveOrderListener = mockk<ApproveOrderListener>(relaxed = true)

    // Ref: https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test#dispatchersmain-delegation
    private lateinit var mainThreadSurrogate: ExecutorCoroutineDispatcher

    @Before
    fun beforeEach() {
        mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)

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
    fun `approveOrder() notifies listener if error fetching clientID`() = runTest {
        val error = PayPalSDKError(123, "fake-description")
        val errorSlot = slot<PayPalSDKError>()

        coEvery { secureTokenServiceAPI.fetchCachedOrRemoteClientID() } throws error
        every {
            approveOrderListener.onApproveOrderFailure(capture(errorSlot))
        } answers { errorSlot.captured }

        val sut = createCardClient(testScheduler)

        sut.approveOrder(mockk(relaxed = true), cardRequest)
        advanceUntilIdle()

        verify {
            approveOrderListener.onApproveOrderFailure(any())
        }
        expectThat(errorSlot.captured) {
            get { code }.isEqualTo(123)
            get { errorDescription }.isEqualTo("Error fetching clientID. Contact developer.paypal.com/support.")
        }
    }

    @Test
    fun `approve order notifies listener of confirm payment source success`() = runTest {
        val sut = createCardClient(testScheduler)

        coEvery { ordersAPI.confirmPaymentSource(cardRequest) } returns confirmPaymentSourceResponse

        sut.approveOrder(activity, cardRequest)
        advanceUntilIdle()

        val resultSlot = slot<CardResult>()
        verify(exactly = 1) { approveOrderListener.onApproveOrderSuccess(capture(resultSlot)) }

        val actual = resultSlot.captured
        assertEquals("sample-order-id", actual.orderID)
    }

    @Test
    fun `approve order notifies listener of confirm payment source failure`() = runTest {
        val sut = createCardClient(testScheduler)

        val error = PayPalSDKError(0, "mock_error_message")
        coEvery { ordersAPI.confirmPaymentSource(cardRequest) } throws error

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
            ConfirmPaymentSourceResponse(orderID, OrderStatus.APPROVED, "/payer/action/href")

        coEvery { ordersAPI.confirmPaymentSource(cardRequest) } returns threeDSecureAuthChallengeResponse

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
    fun `handle browser switch result fetches updated order using browser switch metadata`() =
        runTest {
            val sut = createCardClient(testScheduler)

            val browserSwitchResult =
                createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, approveOrderMetadata)
            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

            sut.handleBrowserSwitchResult(activity)
            advanceUntilIdle()

            val orderRequestSlot = slot<GetOrderRequest>()
            coVerify(exactly = 1) { ordersAPI.getOrderInfo(capture(orderRequestSlot)) }

            val orderRequest = orderRequestSlot.captured
            assertEquals("sample-order-id", orderRequest.orderId)
        }

    @Test
    fun `handle browser switch result notifies user of success with updated order info`() =
        runTest {
            val sut = createCardClient(testScheduler)

            val browserSwitchResult =
                createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, approveOrderMetadata)
            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

            val response =
                GetOrderInfoResponse("sample-order-id", OrderStatus.APPROVED, OrderIntent.CAPTURE)
            coEvery { ordersAPI.getOrderInfo(any()) } returns response

            sut.handleBrowserSwitchResult(activity)
            advanceUntilIdle()

            val cardResultSlot = slot<CardResult>()
            coVerify(exactly = 1) {
                approveOrderListener.onApproveOrderSuccess(capture(cardResultSlot))
            }

            val cardResult = cardResultSlot.captured
            assertEquals("sample-order-id", cardResult.orderID)
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
    fun `handle browser switch result notifies listener of get order failure`() =
        runTest {
            val sut = createCardClient(testScheduler)

            val browserSwitchResult =
                createBrowserSwitchResult(BrowserSwitchStatus.SUCCESS, approveOrderMetadata)
            every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

            val error = PayPalSDKError(0, "mock_error_message")
            coEvery { ordersAPI.getOrderInfo(any()) } throws error

            sut.handleBrowserSwitchResult(activity)
            advanceUntilIdle()

            val errorSlot = slot<PayPalSDKError>()
            verify(exactly = 1) { approveOrderListener.onApproveOrderFailure(capture(errorSlot)) }

            val capturedError = errorSlot.captured
            assertEquals("mock_error_message", capturedError.errorDescription)
        }

    private fun createCardClient(testScheduler: TestCoroutineScheduler): CardClient {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sut = CardClient(
            activity,
            ordersAPI,
            secureTokenServiceAPI,
            analyticsService,
            browserSwitchClient,
            dispatcher
        )
        sut.approveOrderListener = approveOrderListener
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
