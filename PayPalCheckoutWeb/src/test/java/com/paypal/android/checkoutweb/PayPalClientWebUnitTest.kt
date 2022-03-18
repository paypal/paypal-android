package com.paypal.android.checkoutweb

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class PayPalClientWebUnitTest {

    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val browserSwitchHelper: BrowserSwitchHelper = mockk(relaxed = true)
    private val activity: FragmentActivity = mockk(relaxed = true)
    private val coreConfig: CoreConfig = mockk(relaxed = true)

    @Test
    fun `approveOrder starts browserSwitchClient with correct parameters`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        val payPalRequest = PayPalRequest("mock_order_id")
        val browserSwitchOptions = mockk<BrowserSwitchOptions>(relaxed = true)

        every {
            browserSwitchHelper.configurePayPalBrowserSwitchOptions(
                any(),
                coreConfig
            )
        } returns browserSwitchOptions

        payPalClient.approveOrder(payPalRequest)

        verify(exactly = 1) { browserSwitchClient.start(activity, browserSwitchOptions) }
    }

    @Test
    fun `handleBrowserSwitchResult delivers SUCCESS when PayerId and order id are not null`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalCheckoutResult>()

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

        verify(exactly = 1) { payPalClient.listener?.onPayPalSuccess(capture(slot)) }
        assertEquals(slot.captured.orderId, orderId)
        assertEquals(slot.captured.payerId, payerId)

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when PayerId is null or blank`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
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

        verify(exactly = 1) { payPalClient.listener?.onPayPalFailure(capture(slot)) }
        assertEquals(slot.captured.errorDescription,
            "Result did not contain the expected data. Payer ID or Order ID is null.")

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when order id is null or blank`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
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

        verify(exactly = 1) { payPalClient.listener?.onPayPalFailure(capture(slot)) }
        assertEquals(slot.captured.errorDescription,
            "Result did not contain the expected data. Payer ID or Order ID is null.")

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when deeplink is null`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns null
        every { browserSwitchResult.requestMetadata } returns mockk()

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalFailure(capture(slot)) }
        assertEquals(slot.captured.errorDescription,
            "An unknown error occurred. Contact developer.paypal.com/support.")

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Failure when metadata is null`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk(relaxed = true)

        val slot = slot<PayPalSDKError>()

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.SUCCESS
        every { browserSwitchResult.deepLinkUrl } returns mockk()
        every { browserSwitchResult.requestMetadata } returns null

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalFailure(capture(slot)) }
        assertEquals(slot.captured.errorDescription,
            "An unknown error occurred. Contact developer.paypal.com/support.")

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult delivers Cancelled when browserSwitchStatus is CANCELLED`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk(relaxed = true)

        val browserSwitchResult = mockk<BrowserSwitchResult>()

        every { browserSwitchResult.status } returns BrowserSwitchStatus.CANCELED

        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 1) { payPalClient.listener?.onPayPalCanceled() }

        assertNullBrowserSwitchResult(payPalClient)
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when browserSwitchResult is null`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk(relaxed = true)

        every { browserSwitchClient.deliverResult(activity) } returns null

        payPalClient.handleBrowserSwitchResult()

        verify { payPalClient.listener?.wasNot(Called) }
    }

    @Test
    fun `handleBrowserSwitchResult doesn't deliver result when listener is null`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = null

        val browserSwitchResult = mockk<BrowserSwitchResult>()
        every { browserSwitchClient.deliverResult(activity) } returns browserSwitchResult

        payPalClient.handleBrowserSwitchResult()

        verify(exactly = 0) { browserSwitchResult.deepLinkUrl }
        verify(exactly = 0) { browserSwitchResult.requestMetadata }
    }

    @Test
    fun `when listener is set and result is null, no result is delivered`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)
        payPalClient.listener = mockk()

        verify { payPalClient.listener?.wasNot(Called) }
    }

    @Test
    fun `when listener is set and result is not null, a result is delivered`() {
        val payPalClient =
            PayPalWebClient(activity, coreConfig, browserSwitchClient, browserSwitchHelper)

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

        PayPalWebClient(activity, coreConfig, "")

        verify { lifeCycle.addObserver(ofType(PayPalLifeCycleObserver::class)) }
    }

    private fun assertNullBrowserSwitchResult(payPalClient: PayPalWebClient) {
        val privateResult: Field = PayPalWebClient::class.java.getDeclaredField("browserSwitchResult")
        privateResult.isAccessible = true
        val result = privateResult.get(payPalClient)
        assertNull(result)
    }
}
