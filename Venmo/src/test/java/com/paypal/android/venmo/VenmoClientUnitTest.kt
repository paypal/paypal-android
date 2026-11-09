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
import com.paypal.android.venmo.errors.VenmoError
import com.paypal.android.venmo.errors.VenmoErrorCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class VenmoClientUnitTest {

    private val coreConfig = mockk<CoreConfig>(relaxed = true)
    private val ccoAPI = mockk<UpdateClientConfigAPI>(relaxed = true)
    private val chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)
    private val analytics = mockk<VenmoAnalytics>(relaxed = true)

    private lateinit var venmoClient: VenmoClient

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every { coreConfig.clientId } returns "test-client-id"
        every { coreConfig.merchantId } returns "test-merchant-id"
        every { coreConfig.coreEnvironment.venmoCheckoutBaseUrl } returns "https://venmo.com/checkout"

        venmoClient = VenmoClient(
            coreConfig = coreConfig,
            ccoAPI = ccoAPI,
            chromeCustomTabsClient = chromeCustomTabsClient,
            analytics = analytics,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ start Tests ============

    @Test
    fun start_withSuccessfulLaunch_returnsSuccess() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoPresentAuthChallengeResult.Success)
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

        assertTrue(result is VenmoPresentAuthChallengeResult.Failure)
        assertEquals("Unable to launch Venmo app or web browser", (result as VenmoPresentAuthChallengeResult.Failure).error.errorDescription)
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

        assertTrue(result is VenmoPresentAuthChallengeResult.Failure)
        assertEquals(exceptionMessage, (result as VenmoPresentAuthChallengeResult.Failure).error.errorDescription)
        verify {
            analytics.notify(VenmoCheckoutEvent.STARTED, orderId)
            analytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, orderId)
        }
    }

    @Test
    fun start_withBlankOrderId_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)

        val result = venmoClient.start(activity, "")

        assertTrue(result is VenmoPresentAuthChallengeResult.Failure)
        assertEquals("Order ID cannot be blank", (result as VenmoPresentAuthChallengeResult.Failure).error.errorDescription)
        verify {
            analytics.notify(VenmoCheckoutEvent.STARTED, "")
            analytics.notify(VenmoCheckoutEvent.FAILED, "")
        }
        verify(exactly = 0) { chromeCustomTabsClient.launch(any(), any()) }
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
    fun finishStart_withCanceledAndNoOrderId_returnsCanceledWithEmptyOrderId() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("canceled", "true")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Canceled)
        assertEquals("", (result as VenmoFinishStartResult.Canceled).orderId)
        verify {
            analytics.notify(VenmoCheckoutEvent.CANCELED, "")
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
        assertEquals(VenmoErrorCode.MALFORMED_RESULT.ordinal, failureResult.error.code)
        assertEquals(VenmoError.malformedResultError.errorDescription, failureResult.error.errorDescription)
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
        assertEquals(VenmoErrorCode.MALFORMED_RESULT.ordinal, failureResult.error.code)
        assertEquals(VenmoError.malformedResultError.errorDescription, failureResult.error.errorDescription)
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, "")
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
        assertEquals(VenmoErrorCode.MALFORMED_RESULT.ordinal, failureResult.error.code)
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, "")
        }
    }

    @Test
    fun finishStart_withNoQueryParams_returnsFailure() {
        val uri = Uri.parse("app://return")
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
        verify {
            analytics.notify(VenmoCheckoutEvent.FAILED, "")
        }
    }

    // ============ vault Tests ============

    @Test
    fun vault_withSuccessfulLaunch_returnsSuccess() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success

        val result = venmoClient.vault(activity, setupTokenId)

        assertTrue(result is VenmoPresentAuthChallengeResult.Success)
    }

    @Test
    fun vault_withActivityNotFound_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.ActivityNotFound

        val result = venmoClient.vault(activity, setupTokenId)

        assertTrue(result is VenmoPresentAuthChallengeResult.Failure)
        assertEquals(
            "Unable to launch Venmo app or web browser",
            (result as VenmoPresentAuthChallengeResult.Failure).error.errorDescription
        )
    }

    @Test
    fun vault_withException_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        val exceptionMessage = "Launch failed"
        every { chromeCustomTabsClient.launch(any(), any()) } throws Exception(exceptionMessage)

        val result = venmoClient.vault(activity, setupTokenId)

        assertTrue(result is VenmoPresentAuthChallengeResult.Failure)
        assertEquals(exceptionMessage, (result as VenmoPresentAuthChallengeResult.Failure).error.errorDescription)
    }

    @Test(expected = IllegalArgumentException::class)
    fun vault_withBlankSetupTokenId_throwsException() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        venmoClient.vault(activity, "")
    }

    @Test
    fun vault_buildsCorrectUriWithQueryParams() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-456"
        var capturedOptions: ChromeCustomTabOptions? = null
        every { chromeCustomTabsClient.launch(activity, any()) } answers {
            capturedOptions = secondArg()
            LaunchChromeCustomTabResult.Success
        }

        venmoClient.vault(activity, setupTokenId)

        val uri = capturedOptions?.launchUri
        assertEquals("https", uri?.scheme)
        assertEquals("venmo.com", uri?.host)
        assertEquals("/checkout", uri?.path)
        assertEquals(setupTokenId, uri?.getQueryParameter("vault_token"))
        assertEquals("in-app", uri?.getQueryParameter("channel"))
        assertNull(uri?.getQueryParameter("token"))
    }

    @Test
    fun vault_doesNotCallUpdateClientConfig() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success

        venmoClient.vault(activity, setupTokenId)

        coVerify(exactly = 0) { ccoAPI.updateClientConfig(any(), any()) }
    }

    // ============ finishVault Tests ============

    @Test
    fun finishVault_withNoIntentData_returnsNoResult() {
        val intent = Intent()

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.NoResult)
    }

    @Test
    fun finishVault_withApprovalTokenIdAndSessionId_returnsSuccess() {
        val approvalTokenId = "approval-token-123"
        val approvalSessionId = "approval-session-456"
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approval_token_id", approvalTokenId)
            .appendQueryParameter("approval_session_id", approvalSessionId)
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.Success)
        val successResult = result as VenmoFinishVaultResult.Success
        assertEquals(approvalTokenId, successResult.approvalTokenId)
        assertEquals(approvalSessionId, successResult.approvalSessionId)
    }

    @Test
    fun finishVault_withApprovalTokenIdAndNoSessionId_returnsSuccessWithNullSessionId() {
        val approvalTokenId = "approval-token-123"
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approval_token_id", approvalTokenId)
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.Success)
        val successResult = result as VenmoFinishVaultResult.Success
        assertEquals(approvalTokenId, successResult.approvalTokenId)
        assertNull(successResult.approvalSessionId)
    }

    @Test
    fun finishVault_withMissingApprovalTokenId_returnsFailure() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approval_session_id", "approval-session-456")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.Failure)
        val failureResult = result as VenmoFinishVaultResult.Failure
        assertEquals(PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal, failureResult.error.code)
        assertTrue(failureResult.error.errorDescription.contains("Approval token ID"))
    }

    @Test
    fun finishVault_withBlankApprovalTokenId_returnsFailure() {
        val uri = Uri.parse("app://return")
            .buildUpon()
            .appendQueryParameter("approval_token_id", "")
            .build()
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.Failure)
    }

    @Test
    fun finishVault_withNoQueryParams_returnsFailure() {
        val uri = Uri.parse("app://return")
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishVault(intent)

        assertTrue(result is VenmoFinishVaultResult.Failure)
    }

    // ============ callback-based start/vault Tests ============

    @Test
    fun start_withCallback_notifiesSuccess() {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success
        val callback = mockk<VenmoResultCallback>(relaxed = true)

        venmoClient.start(activity, orderId, callback)

        val resultSlot = slot<VenmoPresentAuthChallengeResult>()
        verify(exactly = 1) { callback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoPresentAuthChallengeResult.Success)
    }

    @Test
    fun start_withCallback_notifiesFailure() {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.ActivityNotFound
        val callback = mockk<VenmoResultCallback>(relaxed = true)

        venmoClient.start(activity, orderId, callback)

        val resultSlot = slot<VenmoPresentAuthChallengeResult>()
        verify(exactly = 1) { callback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoPresentAuthChallengeResult.Failure)
    }

    @Test
    fun vault_withCallback_notifiesSuccess() {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success
        val callback = mockk<VenmoResultCallback>(relaxed = true)

        venmoClient.vault(activity, setupTokenId, callback)

        val resultSlot = slot<VenmoPresentAuthChallengeResult>()
        verify(exactly = 1) { callback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoPresentAuthChallengeResult.Success)
    }

    @Test
    fun vault_withCallback_notifiesFailure() {
        val activity = mockk<Activity>(relaxed = true)
        val setupTokenId = "setup-token-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.ActivityNotFound
        val callback = mockk<VenmoResultCallback>(relaxed = true)

        venmoClient.vault(activity, setupTokenId, callback)

        val resultSlot = slot<VenmoPresentAuthChallengeResult>()
        verify(exactly = 1) { callback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoPresentAuthChallengeResult.Failure)
    }
}
