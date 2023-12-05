package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    companion object {
        const val MOCK_RETURN_URL = "com.example.app://vault/success"
        const val MOCK_CANCEL_URL = "com.example.app://vault/cancel"
    }

    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val browserSwitchHelper: BrowserSwitchHelper = mockk(relaxed = true)
    private val activity: FragmentActivity = mockk(relaxed = true)
    private val coreConfig: CoreConfig = mockk(relaxed = true)

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)

    @Test
    fun `start() starts browserSwitchClient with correct parameters`() = runTest {
        val sut = getPayPalCheckoutClient(testScheduler = testScheduler)
        val browserSwitchOptions = mockk<BrowserSwitchOptions>(relaxed = true)

        coEvery {
            browserSwitchHelper.configurePayPalBrowserSwitchOptions(any(), any(), any())
        } returns browserSwitchOptions

        sut.start(mockk(relaxed = true))
        advanceUntilIdle()

        verify(exactly = 1) { browserSwitchClient.start(activity, browserSwitchOptions) }
    }

    @Test
    fun `handleBrowserSwitchResult delivers SUCCESS when PayerId and order id are not null`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalWebCheckoutResult>()

        val payerId = "fake_payer_id"
        val orderId = "fake_order_id"
        val browserSwitchResult = mockk<BrowserSwitchResult>()
        val url = "http://testurl.com/checkout?PayerID=$payerId"
        val metadata = JSONObject()
        metadata.put("order_id", orderId)

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns Uri.parse(url)
        every { browserSwitchResult.requestMetadata } returns metadata

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebSuccess(capture(slot)) }
        assertEquals(slot.captured.orderId, orderId)
        assertEquals(slot.captured.payerId, payerId)

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when PayerId is null or blank`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val payerId = ""
        val orderId = "fake_order_id"
        val browserSwitchResult = mockk<BrowserSwitchResult>()
        val url = "http://testurl.com/checkout?PayerID=$payerId"
        val metadata = JSONObject()
        metadata.put("order_id", orderId)

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns Uri.parse(url)
        every { browserSwitchResult.requestMetadata } returns metadata

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebFailure(capture(slot)) }
        assertEquals(
            slot.captured.errorDescription,
            "Result did not contain the expected data. Payer ID or Order ID is null."
        )

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when order id is null or blank`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val payerId = "fake_payer_id"
        val orderId = ""
        val browserSwitchResult = mockk<BrowserSwitchResult>()
        val url = "http://testurl.com/checkout?PayerID=$payerId"
        val metadata = JSONObject()
        metadata.put("order_id", orderId)

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns Uri.parse(url)
        every { browserSwitchResult.requestMetadata } returns metadata

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebFailure(capture(slot)) }
        assertEquals(
            slot.captured.errorDescription,
            "Result did not contain the expected data. Payer ID or Order ID is null."
        )

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when deeplink is null`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns null
        every { browserSwitchResult.requestMetadata } returns mockk()

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebFailure(capture(slot)) }
        assertEquals(
            slot.captured.errorDescription,
            "An unknown error occurred. Contact developer.paypal.com/support."
        )

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when metadata is null`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns Uri.parse("example.com://paypal/success")
        every { browserSwitchResult.requestMetadata } returns null

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebFailure(capture(slot)) }
        assertEquals(
            slot.captured.errorDescription,
            "An unknown error occurred. Contact developer.paypal.com/support."
        )

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Cancelled when browserSwitchStatus is CANCELLED`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.CANCELED
        every { browserSwitchResult.deepLinkUrl } returns null

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalWebCanceled() }

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when browserSwitchResult is null`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk(relaxed = true)

        every { browserSwitchClient.deliverResult(activity) } returns null

        payPalClient.handleBrowserSwitchResult()

        verify { payPalClient.listener?.wasNot(Called) }
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when listener is null`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = null

        val browserSwitchResult = mockk<BrowserSwitchResult>()
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 0) { browserSwitchResult.deepLinkUrl }
        verify(exactly = 0) { browserSwitchResult.requestMetadata }
    }

    @Test
    fun `when listener is set and result is null, no result is delivered`() {
        val payPalClient = getPayPalCheckoutClient()
        payPalClient.listener = mockk()

        verify { payPalClient.listener?.wasNot(Called) }
    }

    @Test
    fun `when listener is set and result is not null, a result is delivered`() {
        val payPalClient = getPayPalCheckoutClient()

        val browserSwitchResult = mockk<BrowserSwitchResult>(relaxed = true)

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult
        payPalClient.handleBrowserSwitchResult()
        payPalClient.listener = mockk(relaxed = true)

        verify(exactly = 2) { browserSwitchClient.deliverResult(any()) }
    }

    @Test
    fun `when client is created, lifecycle observer is added`() {
        val lifeCycle = mockk<Lifecycle>(relaxed = true)
        every { activity.lifecycle } returns lifeCycle

        PayPalWebCheckoutClient(activity, coreConfig, "")

        verify { lifeCycle.addObserver(ofType(PayPalWebCheckoutLifeCycleObserver::class)) }
    }

    private fun assertNullBrowserSwitchResult(payPalClient: PayPalWebCheckoutClient) {
        val privateResult: Field =
            PayPalWebCheckoutClient::class.java.getDeclaredField("browserSwitchResult")
        privateResult.isAccessible = true
        val result = privateResult.get(payPalClient)
        assertNull(result)
    }

    private fun getPayPalCheckoutClient(
        coreConfig: CoreConfig = CoreConfig("fake-client-id"),
        testScheduler: TestCoroutineScheduler? = null
    ): PayPalWebCheckoutClient {
        return (testScheduler?.let {
            val dispatcher = StandardTestDispatcher(testScheduler)
            PayPalWebCheckoutClient(
                activity,
                coreConfig,
                analyticsService,
                browserSwitchClient,
                browserSwitchHelper,
                PayPalWebCheckoutVaultExperienceContext(MOCK_RETURN_URL, MOCK_CANCEL_URL),
                dispatcher
            )
        } ?: PayPalWebCheckoutClient(
            activity,
            coreConfig,
            analyticsService,
            browserSwitchClient,
            browserSwitchHelper,
            PayPalWebCheckoutVaultExperienceContext(MOCK_RETURN_URL, MOCK_CANCEL_URL),
        ))
    }
}
