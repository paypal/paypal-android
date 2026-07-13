package com.paypal.android.venmo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PayPalSDKErrorCode
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.GetFundingEligibility
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabOptions
import com.paypal.android.corepayments.browserswitch.ChromeCustomTabsClient
import com.paypal.android.corepayments.browserswitch.LaunchChromeCustomTabResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.FundingEligibility
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val coreConfig = mockk<CoreConfig>(relaxed = true)
    private val ccoAPI = mockk<UpdateClientConfigAPI>(relaxed = true)
    private val getFundingEligibility = mockk<GetFundingEligibility>(relaxed = true)
    private val chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)

    private lateinit var venmoClient: VenmoClient

    @Before
    fun setUp() {
        every { coreConfig.clientId } returns "test-client-id"
        every { coreConfig.merchantId } returns "test-merchant-id"
        every { coreConfig.environment.venmoBaseUrl } returns "https://venmo.com/checkout"
        every { coreConfig.environment.venmoEnvironment } returns "sandbox"

        venmoClient = VenmoClient(
            context = context,
            coreConfig = coreConfig,
            ccoAPI = ccoAPI,
            getFundingEligibility = getFundingEligibility,
            chromeCustomTabsClient = chromeCustomTabsClient
        )
    }

    // ============ isEligible Tests ============

    @Test
    fun isEligible_withEligibleVenmo_returnsEligible() = runTest {
        val buyerCountry = "US"
        val fundingEligibility = mockk<FundingEligibility> {
            every { venmoEligible } returns true
        }
        coEvery { getFundingEligibility(any(), any(), any(), any(), any()) } returns
                APIResult.Success(fundingEligibility)

        val result = venmoClient.isEligible(buyerCountry)

        assertTrue(result is VenmoEligibilityResult.Eligible)
    }

    @Test
    fun isEligible_withIneligibleVenmo_returnsIneligible() = runTest {
        val buyerCountry = "US"
        val fundingEligibility = mockk<FundingEligibility> {
            every { venmoEligible } returns false
        }
        coEvery { getFundingEligibility(any(), any(), any(), any(), any()) } returns
                APIResult.Success(fundingEligibility)

        val result = venmoClient.isEligible(buyerCountry)

        assertTrue(result is VenmoEligibilityResult.Ineligible)
        assertEquals("Venmo is not eligible for this transaction", (result as VenmoEligibilityResult.Ineligible).reason)
    }

    @Test
    fun isEligible_withAPIFailure_returnsError() = runTest {
        val buyerCountry = "US"
        val error = PayPalSDKError(
            code = PayPalSDKErrorCode.UNKNOWN.ordinal,
            errorDescription = "API Error"
        )
        coEvery { getFundingEligibility(any(), any(), any(), any(), any()) } returns
                APIResult.Failure(error)

        val result = venmoClient.isEligible(buyerCountry)

        assertTrue(result is VenmoEligibilityResult.Error)
        assertEquals(error, (result as VenmoEligibilityResult.Error).error)
    }

    @Test
    fun isEligible_withException_returnsError() = runTest {
        val buyerCountry = "US"
        val exceptionMessage = "Network error"
        coEvery { getFundingEligibility(any(), any(), any(), any(), any()) } throws
                Exception(exceptionMessage)

        val result = venmoClient.isEligible(buyerCountry)

        assertTrue(result is VenmoEligibilityResult.Error)
        assertEquals(exceptionMessage, (result as VenmoEligibilityResult.Error).error.errorDescription)
    }

    @Test(expected = IllegalArgumentException::class)
    fun isEligible_withBlankBuyerCountry_throwsException() = runTest {
        venmoClient.isEligible("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun isEligible_withBlankClientId_throwsException() = runTest {
        every { coreConfig.clientId } returns ""
        venmoClient.isEligible("US")
    }

    @Test
    fun isEligible_callsGetFundingEligibilityWithCorrectParams() = runTest {
        val buyerCountry = "CA"
        val fundingEligibility = mockk<FundingEligibility> {
            every { venmoEligible } returns true
        }
        coEvery { getFundingEligibility(context, "test-client-id", listOf("VENMO"), listOf("test-merchant-id"), buyerCountry) } returns
                APIResult.Success(fundingEligibility)

        venmoClient.isEligible(buyerCountry)

        coVerify {
            getFundingEligibility(
                context = context,
                clientId = "test-client-id",
                fundingSources = listOf("VENMO"),
                merchantIds = listOf("test-merchant-id"),
                buyerCountry = buyerCountry
            )
        }
    }

    // ============ start Tests ============

    @Test
    fun start_withSuccessfulLaunch_returnsSuccess() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.Success

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoStartResult.Success)
    }

    @Test
    fun start_withActivityNotFound_returnsFailure() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val orderId = "order-123"
        every { chromeCustomTabsClient.launch(any(), any()) } returns LaunchChromeCustomTabResult.ActivityNotFound

        val result = venmoClient.start(activity, orderId)

        assertTrue(result is VenmoStartResult.Failure)
        assertEquals("Unable to launch Venmo app or web browser", (result as VenmoStartResult.Failure).error.errorDescription)
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
                fundingSource = "VENMO"
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
        assertEquals("sandbox", uri?.getQueryParameter("env"))
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
        assertTrue(failureResult.error.errorDescription.contains("payerId"))
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
    }

    @Test
    fun finishStart_withNoQueryParams_returnsFailure() {
        val uri = Uri.parse("app://return")
        val intent = Intent().apply { data = uri }

        val result = venmoClient.finishStart(intent)

        assertTrue(result is VenmoFinishStartResult.Failure)
    }
}
