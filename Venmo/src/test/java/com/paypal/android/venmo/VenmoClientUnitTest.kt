package com.paypal.android.venmo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabOptions
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabsClient
import com.paypal.android.corepayments.browserswitch.LaunchChromeCustomTabResult
import com.paypal.android.venmo.analytics.VenmoAnalytics
import com.paypal.android.venmo.analytics.VenmoCheckoutEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoClientUnitTest {

    private val coreConfig = mockk<CoreConfig>(relaxed = true)
    private val ccoAPI = mockk<UpdateClientConfigAPI>(relaxed = true)
    private val chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)
    private val analytics = mockk<VenmoAnalytics>(relaxed = true)

    private lateinit var venmoClient: VenmoClient

    @Before
    fun setUp() {
        every { coreConfig.clientId } returns "test-client-id"
        every { coreConfig.merchantId } returns "test-merchant-id"
        every { coreConfig.coreEnvironment.venmoCheckoutBaseUrl } returns "https://venmo.com/checkout"

        venmoClient = VenmoClient(
            coreConfig = coreConfig,
            ccoAPI = ccoAPI,
            chromeCustomTabsClient = chromeCustomTabsClient,
            analytics = analytics
        )
    }

    // ============ start Tests ============

    @Test
    fun start_withSuccessfulLaunch_returnsSuccess() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoStartResult.Success)
        verify {
            analytics.notify(VenmoCheckoutEvent.STARTED, orderId)
            analytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED, orderId)
        }
    }

    @Test
    fun start_withActivityNotFound_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.ActivityNotFound

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoStartResult.Failure)
        assertEquals("Unable to launch Venmo app or web browser", (result as VenmoStartResult.Failure).error.errorDescription)
        verify {
            analytics.notify(VenmoCheckoutEvent.STARTED, orderId)
            analytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, orderId)
        }
    }

    @Test
    fun start_withException_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        val exceptionMessage = "Launch failed"
        every { chromeCustomTabsClient.launch(any(), any()) } throws Exception(exceptionMessage)

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoStartResult.Failure)
        assertEquals(exceptionMessage, (result as VenmoStartResult.Failure).error.errorDescription)
        verify {
            analytics.notify(VenmoCheckoutEvent.STARTED, orderId)
            analytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, orderId)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun start_withBlankOrderId_throwsException() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        venmoClient.start(activity, "")
    }

    @Test
    fun start_callsUpdateClientConfig() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success
        coEvery { ccoAPI.updateClientConfig(orderId, "VENMO") } returns mockk()

        venmoClient.start(activity, orderId)

        coVerify {
            ccoAPI.updateClientConfig(
                tokenId = orderId,
                fundingSource = "venmo"
            )
        }
    }

    @Test
    fun start_buildsCorrectUriWithQueryParams() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-456"
        var capturedOptions: ChromeCustomTabOptions? = null
        every { chromeCustomTabsClient.launch(activity, any()) } answers {
            capturedOptions = secondArg()
            LaunchChromeCustomTabResult.Success
        }

        venmoClient.start(activity, orderId)

        val uri = capturedOptions?.launchUri
        assertEquals("https", uri?.scheme)
        assertEquals("venmo.com", uri?.host)
        assertEquals("/checkout", uri?.path)
        assertEquals(orderId, uri?.getQueryParameter("token"))
        assertEquals("in-app", uri?.getQueryParameter("channel"))
        assertNull(uri?.getQueryParameter("env"))
    }

    // ============ finishStart Tests ============

    @Test
    fun finishStart_withNoIntentData_returnsNoResult() {
        val intent = Intent()

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.NoResult)
    }

    @Test
    fun finishStart_withCanceledParam_returnsCanceled() {
        val orderId = "order-123"
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("canceled", "true")
            .appendQueryParameter("token", orderId)
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Canceled)
        assertEquals(orderId, (result as VenmoFinishStartResult.Canceled).orderId)
        verify {
            analytics.notify(VenmoCheckoutEvent.CANCELED, orderId)
        }
    }

    @Test
    fun finishStart_withCanceledAndNoOrderId_returnsCanceledWithNull() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("canceled", "true")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Canceled)
        assertNull((result as VenmoFinishStartResult.Canceled).orderId)
        verify {
            analytics.notify(VenmoCheckoutEvent.CANCELED, null)
        }
    }

    @Test
    fun finishStart_withApprovedAndValidPayerId_returnsSuccess() {
        val orderId = "order-123"
        val payerId = "payer-456"
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approved", "true")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("PayerID", payerId)
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Success)
        val successResult = result as VenmoFinishStartResult.Success
        assertEquals(orderId, successResult.token)
        assertEquals(payerId, successResult.payerId)
        assertEquals(true, successResult.approved)
        verify {
            analytics.notify(VenmoCheckoutEvent.SUCCEEDED, orderId)
        }
    }

    @Test
    fun finishStart_withApprovedButMissingPayerId_returnsFailure() {
        val orderId = "order-123"
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approved", "true")
            .appendQueryParameter("token", orderId)
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
        val failureResult = result as VenmoFinishStartResult.Failure
        assertEquals(PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal, failureResult.error.code)
        assertTrue(failureResult.error.errorDescription.contains("Payer ID"))
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, orderId)
        }
    }

    @Test
    fun finishStart_withApprovedButMissingOrderId_returnsFailure() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approved", "true")
            .appendQueryParameter("PayerID", "payer-456")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
        val failureResult = result as VenmoFinishStartResult.Failure
        assertEquals(PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal, failureResult.error.code)
        assertTrue(failureResult.error.errorDescription.contains("Order ID"))
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, null)
        }
    }

    @Test
    fun finishStart_withNeither_CanceledNor_ApprovedTrue_returnsFailure() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approved", "false")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
        val failureResult = result as VenmoFinishStartResult.Failure
        assertEquals(PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal, failureResult.error.code)
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, null)
        }
    }

    @Test
    fun finishStart_withNoQueryParams_returnsFailure() {
        val uri = Uri.parse("app://return")
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, null)
        }
    }
}
