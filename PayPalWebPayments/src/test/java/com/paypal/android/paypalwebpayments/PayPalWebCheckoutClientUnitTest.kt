package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)
    private lateinit var patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility
    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val urlScheme = "com.example.app"
    private val testDispatcher = StandardTestDispatcher()
    private val fakeAppSwitchUrl = "https://paypal.com/vault-app-switch"

    private val intent = Intent()

    private lateinit var payPalWebLauncher: PayPalWebLauncher
    private lateinit var sut: PayPalWebCheckoutClient

    @Before
    fun beforeEach() {
        payPalWebLauncher = mockk(relaxed = true)
        patchCCOWithAppSwitchEligibility = mockk(relaxed = true)
        sut = PayPalWebCheckoutClient(
            analytics,
            payPalWebLauncher,
            patchCCOWithAppSwitchEligibility,
            coreConfig,
            urlScheme,
            testDispatcher
        )
    }

    @Test
    fun `start() fetches client token and launches PayPal web checkout`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        sut.start(activity, request)

        // Verify launchWithUrl is called with correct parameters
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
            )
        }
    }

    @Test
    fun `start() notifies merchant of browser switch failure`() = runTest {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebCheckoutRequest("fake-order-id")
        val result = sut.start(activity, request)
        assertSame(launchResult, result)
    }

    // Note: Authentication failure tests removed since authentication
    // is now handled internally by PatchCCOWithAppSwitchEligibility

    @Test
    fun `vault() launches PayPal web checkout`() = runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebVaultRequest("fake-setup-token-id")
        sut.vault(activity, request)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
    }

    @Test
    fun `vault() notifies merchant of browser switch failure`() = runTest {
        val sdkError = PayPalSDKError(123, "fake error description")
        val launchResult = PayPalPresentAuthChallengeResult.Failure(sdkError)
        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        val request = PayPalWebVaultRequest("fake-setup-token-id")
        val result = sut.vault(activity, request) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `finishStart() forwards success result from auth launcher`() {
        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() forwards error result from auth launcher`() {

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() forwards cancellation result from auth launcher`() {
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order_id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishVault() forwards vault success from PayPal web launcher`() {
        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Success

        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() notifies merchant of vault failure`() {
        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Failure

        assertSame(error, result.error)
    }

    @Test
    fun `finishVault forwards vault cancellation`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `finishVault forwards no result`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }

    @Test
    fun `start() uses app switch when enabled and app switch URL is available`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse(fakeAppSwitchUrl)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = any(),
                tokenType = any(),
                returnUrlScheme = urlScheme
            )
        } returns launchResult

        // When
        val result = sut.start(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                activity.applicationContext,
                "fake-order-id",
                TokenType.ORDER_ID,
                true,
                true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() falls back to web checkout when app switch is enabled but URL is null`() =
        runTest {
            // Given
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
            val appSwitchResponse = createAppSwitchEligibilityResponse(null)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.start(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnUrlScheme = urlScheme
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() falls back to web checkout when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.start(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnUrlScheme = urlScheme
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `start() falls back to web checkout when app switch request fails`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = true)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Failure(PayPalSDKError(1001, "Test failure"))

        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        val result = sut.start(activity, request)

        // Then
        coVerify { patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any()) }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest("fake-order-id", appSwitchWhenEligible = false)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        val result = sut.start(activity, request)

        // Then
        coVerify(exactly = 0) {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `start() uses correct token type in app switch request`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest("test-order-123", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse("https://test.com")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("state")

        // When
        sut.start(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "test-order-123",
                tokenType = TokenType.ORDER_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
    }

    // VAULT APP SWITCH TESTS

    @Test
    fun `vault() uses app switch when enabled and app switch URL is available`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
        val appSwitchResponse =
            createAppSwitchEligibilityResponse(fakeAppSwitchUrl)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = fakeAppSwitchUrl.toUri(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        } returns launchResult

        // When
        val result = sut.vault(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() falls back to web vault when app switch is enabled but URL is null`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse(null)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        // When
        val result = sut.vault(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                activity.applicationContext, "fake-setup-token-id", TokenType.VAULT_ID, true, true
            )
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() falls back to web vault when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vault(activity, request)

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnUrlScheme = urlScheme
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vault() falls back to web vault when app switch request fails`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = true)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Failure(PayPalSDKError(1001, "Test vault failure"))

        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        val result = sut.vault(activity, request)

        // Then
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("fake-setup-token-id", appSwitchWhenEligible = false)
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        every {
            payPalWebLauncher.launchWithUrl(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns launchResult

        // When
        val result = sut.vault(activity, request)

        // Then
        coVerify(exactly = 0) {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        }
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnUrlScheme = urlScheme
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vault() uses correct token type in app switch request`() = runTest {
        // Given
        val request = PayPalWebVaultRequest("test-setup-123", appSwitchWhenEligible = true)
        val appSwitchResponse = createAppSwitchEligibilityResponse("https://test.com")

        coEvery {
            patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
        } returns APIResult.Success(appSwitchResponse)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("state")

        // When
        sut.vault(activity, request)

        // Then
        coVerify {
            patchCCOWithAppSwitchEligibility(
                context = activity.applicationContext,
                orderId = "test-setup-123",
                tokenType = TokenType.VAULT_ID,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true
            )
        }
    }

    private fun createAppSwitchEligibilityResponse(redirectURL: String?): AppSwitchEligibility {
        return AppSwitchEligibility(
            appSwitchEligible = true,
            launchUrl = redirectURL,
            ineligibleReason = null
        )
    }
}
