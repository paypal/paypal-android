package com.paypal.android.paypalwebpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityData
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PayPalWebCheckoutClientUnitTest {

    @MockK
    private val activity: FragmentActivity = mockk(relaxed = true)

    @MockK
    private val analytics = mockk<PayPalWebAnalytics>(relaxed = true)

    @MockK
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility =
        mockk(relaxed = true)

    @MockK
    private val deviceInspector: DeviceInspector = mockk(relaxed = true)
    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val urlScheme = "com.example.app"

    @MockK
    private val updateClientConfigAPI: UpdateClientConfigAPI = mockk(relaxed = true)
    private val appLinkUrl = "https://example.com/"
    private val cancelAppUrl = "https://example.com/cancel"

    private val returnToAppUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = appLinkUrl,
        cancelAppUrl = cancelAppUrl,
        fallbackSchemeUrl = urlScheme
    )

    private val intent = Intent()

    @MockK
    private val payPalWebLauncher: PayPalWebLauncher = mockk(relaxed = true)
    private lateinit var sut: PayPalWebCheckoutClient

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun beforeEach() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig
        )
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startWithOrderId() launches PayPal web checkout`() = runTest {
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

        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)

        // Verify launchWithUrl is called with correct parameters
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
    }

    @Test
    fun `startWithOrderId() notifies merchant of browser switch failure`() = runTest {
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

        val customReturnUrl = "https://example.com/return"
        val config = ReturnToAppUrlConfig(
            returnAppUrl = customReturnUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            config
        )
        val result = sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        assertSame(launchResult, result)
    }

    @Test
    fun `vaultWithSetupTokenId() launches PayPal web vault`() = runTest {
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

        val vaultReturnUrl = "https://example.com/vault/return"
        val vaultConfig = ReturnToAppUrlConfig(
            returnAppUrl = vaultReturnUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            vaultConfig
        )
        sut.vaultWithSetupTokenId(activity, "fake-setup-token-id", request.returnToAppUrlConfig)
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(vaultReturnUrl)
            )
        }
    }

    @Test
    fun `vaultWithSetupTokenId() notifies merchant of browser switch failure`() = runTest {
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

        val vaultReturnUrl = "https://example.com/vault/return"
        val vaultConfig = ReturnToAppUrlConfig(
            returnAppUrl = vaultReturnUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            vaultConfig
        )
        val result = sut.vaultWithSetupTokenId(
            activity,
            "fake-setup-token-id",
            request.returnToAppUrlConfig
        ) as PayPalPresentAuthChallengeResult.Failure

        assertSame(sdkError, result.error)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards success result from auth launcher`() {
        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards error result from auth launcher`() {
        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with merchant provided auth state forwards cancellation result from auth launcher`() {
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        val result = sut.finishStart(intent, "auth state")
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state forwards success result from auth launcher`() =
        runTest {
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

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards success result from auth launcher`() =
        runTest {
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

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            val launchWithUrlClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
        )
        val request = PayPalWebCheckoutRequest(PayPalUserIdentity.Unknown, returnToAppUrlConfig)
            launchWithUrlClient.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            sut.restore(launchWithUrlClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(successResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards error result from auth launcher`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards error result from auth launcher`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        val result = sut.finishStart(intent)
        assertSame(failureResult, result)
    }

    @Test
    fun `finishStart() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
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

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
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

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            val launchWithUrlClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
                deviceInspector = deviceInspector,
                coreConfig = coreConfig,
                urlScheme = urlScheme,
                patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
        )
        val request = PayPalWebCheckoutRequest(PayPalUserIdentity.Unknown, returnToAppUrlConfig)
            launchWithUrlClient.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            sut.restore(launchWithUrlClient.instanceState)
        val result = sut.finishStart(intent)
        assertSame(canceledResult, result)
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
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

        val successResult =
            PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns successResult

            sut.startWithOrderId(activity, "fake-order-id", returnToAppUrlConfig)
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

            sut.startWithOrderId(activity, "fake-order-id", returnToAppUrlConfig)
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
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

        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

            sut.startWithOrderId(activity, "fake-order-id", returnToAppUrlConfig)
        sut.finishStart(intent)
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishVault() with merchant provided auth forwards vault success from PayPal web launcher`() {
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
    fun `finishVault() with merchant provided auth notifies merchant of vault failure`() {
        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

        val result = sut.finishVault(intent, "auth state")
                as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault with merchant provided auth forwards vault cancellation`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.Canceled)
    }

    @Test
    fun `finishVault with merchant provided auth forwards no result`() {
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.NoResult

        val result = sut.finishVault(intent, "auth state")
        assertTrue(result is PayPalWebCheckoutFinishVaultResult.NoResult)
    }

    @Test
    fun `startWithOrderId() falls back to web checkout when app switch is enabled but URL is null`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse(null)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `startWithOrderId() falls back to web checkout when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `startWithOrderId() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
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
        val result = sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)

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
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    // VAULT APP SWITCH TESTS

    @Test
    fun `vaultWithSetupTokenId() falls back to web vault when app switch is enabled but empty URL is returned`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val vaultConfig = ReturnToAppUrlConfig(
                returnAppUrl = appLinkUrl,
                cancelAppUrl = cancelAppUrl,
                fallbackSchemeUrl = urlScheme
            )
            val request = PayPalWebVaultRequest(
                PayPalUserIdentity.Unknown,
                vaultConfig
            )
            val appSwitchResponse = createAppSwitchEligibilityResponse("")
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(appSwitchResponse)

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                request.returnToAppUrlConfig
            )

            // Then
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vaultWithSetupTokenId() falls back to web vault when app switch request fails`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns true
        val vaultConfig = ReturnToAppUrlConfig(
            returnAppUrl = appLinkUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            vaultConfig
        )
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
        val result = sut.vaultWithSetupTokenId(
            activity,
            "fake-setup-token-id",
            request.returnToAppUrlConfig
        )

        // Then
        verify {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `vaultWithSetupTokenId() skips app switch check when appSwitchWhenEligible is false`() = runTest {
        // Given
        val vaultConfig = ReturnToAppUrlConfig(
            returnAppUrl = appLinkUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            vaultConfig
        )
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
        val result = sut.vaultWithSetupTokenId(
            activity,
            "fake-setup-token-id",
            request.returnToAppUrlConfig
        )

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
                returnToAppStrategy = any()
            )
        }
        assertSame(launchResult, result)
    }

    @Test
    fun `startWithOrderId() skips app switch when PayPal app not installed`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vaultWithSetupTokenId() skips app switch when PayPal app not installed`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val vaultConfig = ReturnToAppUrlConfig(
                returnAppUrl = appLinkUrl,
                cancelAppUrl = cancelAppUrl,
                fallbackSchemeUrl = urlScheme
            )
            val request = PayPalWebVaultRequest(
                PayPalUserIdentity.Unknown,
                vaultConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                request.returnToAppUrlConfig
            )

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `startWithOrderId() uses web checkout when app switch disabled regardless of PayPal app installation`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.startWithOrderId(
                activity,
                "fake-order-id",
                request.returnToAppUrlConfig
            )

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `vaultWithSetupTokenId() uses web vault when app switch disabled regardless of PayPal app installation`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns true
            val vaultConfig = ReturnToAppUrlConfig(
                returnAppUrl = appLinkUrl,
                cancelAppUrl = cancelAppUrl,
                fallbackSchemeUrl = urlScheme
            )
            val request = PayPalWebVaultRequest(
                PayPalUserIdentity.Unknown,
                vaultConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            val result = sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                request.returnToAppUrlConfig
            )

            // Then
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility.invoke(any(), any(), any(), any(), any())
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    activity = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            assertSame(launchResult, result)
        }

    @Test
    fun `finishVault() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state forwards success result from auth launcher`() =
        runTest {
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
        )

            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

            previousClient.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )

            sut.restore(previousClient.instanceState)
            val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
            assertSame("fake-approval-session-id", result.approvalSessionId)
        }

    @Test
    fun `finishVault() with restored session auth state forwards success result from auth launcher`() =
        runTest {
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

        val previousClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility
        )

            every {
                payPalWebLauncher.launchWithUrl(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns successResult

            previousClient.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )

        sut.restore(previousClient.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
        assertSame("fake-approval-session-id", result.approvalSessionId)
    }

    @Test
    fun `finishVault() with session auth state forwards error result from auth launcher`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with restored session auth state forwards error result from auth launcher`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )

            sut.restore(sut.instanceState)
        val result = sut.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
        assertSame(error, result.error)
    }

    @Test
    fun `finishVault() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
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

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
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

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                ReturnToAppUrlConfig(
                    returnAppUrl = appLinkUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = urlScheme
                )
            )

            sut.restore(sut.instanceState)
        val result = sut.finishVault(intent)
        assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
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

        val successResult =
            PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns successResult

            sut.vaultWithSetupTokenId(activity, "fake-setup-token-id", returnToAppUrlConfig)
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
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

        val error = PayPalSDKError(123, "fake-error-description")
        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sut.vaultWithSetupTokenId(activity, "fake-setup-token-id", returnToAppUrlConfig)
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
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

        every {
            payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
        } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sut.vaultWithSetupTokenId(activity, "fake-setup-token-id", returnToAppUrlConfig)
        sut.finishVault(intent)
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `startWithOrderId() uses AppLink from returnToAppUrlConfig`() = runTest {
        val customReturnUrl = "https://example.com/return"
        val config = ReturnToAppUrlConfig(
            returnAppUrl = customReturnUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        sut.startWithOrderId(activity, "fake-order-id", config)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(customReturnUrl)
            )
        }
    }

    @Test
    fun `vaultWithSetupTokenId() uses AppLink from returnToAppUrlConfig`() = runTest {
        val vaultReturnUrl = "https://example.com/vault/return"
        val config = ReturnToAppUrlConfig(
            returnAppUrl = vaultReturnUrl,
            cancelAppUrl = cancelAppUrl,
            fallbackSchemeUrl = urlScheme
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        sut.vaultWithSetupTokenId(activity, "fake-setup-token-id", config)

        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(vaultReturnUrl)
            )
        }
    }

    // MARK: - Tests for Callback-based Methods

    @Test
    fun `start() with callback method exists and can be called`() {
        // This test just verifies the callback method can be called without throwing
        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val createOrderHandler = CreateOrderHandler {
            CreateOrderResponse.Success("fake-order-id")
        }

        // Mock the async dependencies to avoid side effects
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        // This should not throw an exception
        sut.start(activity, request, createOrderHandler, callback)
    }

    @Test
    fun `vault() with callback method exists and can be called`() {
        // This test just verifies the callback method can be called without throwing
        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val createSetupTokenHandler = CreateSetupTokenHandler {
            CreateSetupTokenResponse.Success("fake-setup-token-id")
        }

        // Mock the async dependencies to avoid side effects
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth state")

        // This should not throw an exception
        sut.vault(activity, request, createSetupTokenHandler, callback)
    }

    // MARK: - Tests for new start()/vault() handler + callback flows

    @Test
    fun `start() calls createOrderHandler on IO thread and launches on success`() = runTest {
        // Given
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val createOrderHandler = CreateOrderHandler {
            CreateOrderResponse.Success("fake-order-id")
        }

        // When
        val sutWithTestScope = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = this
        )
        sutWithTestScope.start(activity, request, createOrderHandler, callback)
        advanceUntilIdle()

        // Then
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
        verify(exactly = 1) { callback.onPayPalWebStartResult(launchResult) }
    }

    @Test
    fun `start() calls callback with error when createOrderHandler returns Failure`() = runTest {
        // Given
        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val cause = Exception("Order creation failed")
        val createOrderHandler = CreateOrderHandler {
            CreateOrderResponse.Failure(cause)
        }

        // When
        val sutWithTestScope = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = this
        )
        sutWithTestScope.start(activity, request, createOrderHandler, callback)
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) }
        verify(exactly = 1) {
            callback.onPayPalWebStartResult(match { result ->
                result is PayPalPresentAuthChallengeResult.Failure
            })
        }
    }

    @Test
    fun `vault() calls createSetupTokenHandler on IO thread and launches on success`() = runTest {
        // Given
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val createSetupTokenHandler = CreateSetupTokenHandler {
            CreateSetupTokenResponse.Success("fake-setup-token-id")
        }

        // When
        val sutWithTestScope = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = this
        )
        sutWithTestScope.vault(activity, request, createSetupTokenHandler, callback)
        advanceUntilIdle()

        // Then
        verify(exactly = 1) {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = ReturnToAppStrategy.AppLink(appLinkUrl)
            )
        }
        verify(exactly = 1) { callback.onPayPalWebVaultResult(launchResult) }
    }

    @Test
    fun `vault() calls callback with error when createSetupTokenHandler returns Failure`() = runTest {
        // Given
        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        val request = PayPalWebVaultRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val cause = Exception("Setup token creation failed")
        val createSetupTokenHandler = CreateSetupTokenHandler {
            CreateSetupTokenResponse.Failure(cause)
        }

        // When
        val sutWithTestScope = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            updateClientConfigAPI = updateClientConfigAPI,
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = this
        )
        sutWithTestScope.vault(activity, request, createSetupTokenHandler, callback)
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) }
        verify(exactly = 1) {
            callback.onPayPalWebVaultResult(match { result ->
                result is PayPalPresentAuthChallengeResult.Failure
            })
        }
    }

    // MARK: - Analytics Tests

    @Test
    fun `startWithOrderId() sends analytics with appSwitchEnabled false when app switch is not used`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val request = PayPalWebCheckoutRequest(
                PayPalUserIdentity.Unknown,
                returnToAppUrlConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)

            // Then
            verify { analytics.notify(CheckoutEvent.STARTED, "fake-order-id", false) }
            verify {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    "fake-order-id",
                    false
                )
            }
        }

    @Test
    fun `finishStart() sends analytics with appSwitchEnabled false for canceled event`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns false
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns canceledResult

        // When
        sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        sut.finishStart(intent)

        // Then
        verify { analytics.notify(CheckoutEvent.CANCELED, "fake-order-id", false) }
    }

    @Test
    fun `finishStart() sends analytics with appSwitchEnabled false for failed event`() = runTest {
        // Given
        every { deviceInspector.isPayPalInstalled } returns false
        val request = PayPalWebCheckoutRequest(
            PayPalUserIdentity.Unknown,
            returnToAppUrlConfig
        )
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
        val error = PayPalSDKError(123, "fake-error-description")
        val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)

        every {
            payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
        } returns launchResult

        every {
            payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
        } returns failureResult

        // When
        sut.startWithOrderId(activity, "fake-order-id", request.returnToAppUrlConfig)
        sut.finishStart(intent)

        // Then
        verify { analytics.notify(CheckoutEvent.FAILED, "fake-order-id", false) }
    }

    @Test
    fun `vaultWithSetupTokenId() sends analytics with appSwitchEnabled false when app switch is not used`() =
        runTest {
            // Given
            every { deviceInspector.isPayPalInstalled } returns false
            val vaultConfig = ReturnToAppUrlConfig(
                returnAppUrl = appLinkUrl,
                cancelAppUrl = cancelAppUrl,
                fallbackSchemeUrl = urlScheme
            )
            val request = PayPalWebVaultRequest(
                PayPalUserIdentity.Unknown,
                vaultConfig
            )
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")

            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            // When
            sut.vaultWithSetupTokenId(
                activity,
                "fake-setup-token-id",
                request.returnToAppUrlConfig
            )

            // Then
            verify { analytics.notify(VaultEvent.STARTED, "fake-setup-token-id", false) }
            verify {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    "fake-setup-token-id",
                    false
                )
            }
        }

    fun createAppSwithEligibility(launchUrl: String?) = AppSwitchEligibilityData(
        appSwitchEligible = !launchUrl.isNullOrEmpty(),
        redirectURL = launchUrl,
        ineligibleReason = if (launchUrl.isNullOrEmpty()) "App switch not eligible" else null
    )

    private fun createAppSwitchEligibilityResponse(redirectURL: String?): AppSwitchEligibility {
        return AppSwitchEligibility(
            appSwitchEligible = true,
            launchUrl = redirectURL,
            ineligibleReason = null
        )
    }
}
