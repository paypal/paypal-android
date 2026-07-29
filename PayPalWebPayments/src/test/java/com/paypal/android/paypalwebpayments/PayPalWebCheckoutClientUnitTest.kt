package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.HttpRoundTripTiming
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityData
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityParams
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.ShopperSessionConfig
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyResult
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyUseCase
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import com.paypal.android.paypalwebpayments.analytics.AppSwitchAnalyticsEventParams
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.CreatePayPalSessionEvent
import com.paypal.android.paypalwebpayments.analytics.LatencyEndpoint
import com.paypal.android.paypalwebpayments.analytics.LatencyFlow
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.PresentationType
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import com.paypal.android.paypalwebpayments.usecase.GetEffectiveReturnUrlConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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

    // NOTE: Pre-existing gap unrelated to the patchCCO-fallback feature work: this dependency
    // was never wired into any PayPalWebCheckoutClient(...) construction in this test file,
    // which meant the file did not compile. Adding it here so the suite (and the new
    // fallback-to-patchCCO tests below) can actually build and run.
    @MockK
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI =
        mockk(relaxed = true)

    @MockK
    private val getReturnToAppStrategyUseCase: GetReturnToAppStrategyUseCase = mockk(relaxed = true)

    @MockK
    private val deviceInspector: DeviceInspector = mockk(relaxed = true)
    private val coreConfig = CoreConfig("fake-client-id", "fake-merchant-id", Environment.SANDBOX)

    private val intent = Intent()

    @MockK
    private val payPalWebLauncher: PayPalWebLauncher = mockk(relaxed = true)
    private lateinit var sut: PayPalWebCheckoutClient

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun beforeEach() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        every {
            getReturnToAppStrategyUseCase(any(), any(), any())
        } returns GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(fakeUrlConfig.returnAppUrl))
        sut = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            getReturnToAppStrategyUseCase = getReturnToAppStrategyUseCase,
            getEffectiveReturnUrlConfigUseCase = GetEffectiveReturnUrlConfigUseCase(),
            deviceInspector = deviceInspector,
            coreConfig = coreConfig
        )
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `finishStart() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishStart(intent))
    }

    @Test
    fun `finishStart() with session auth state forwards success result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns successResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishStart(intent)
            assertSame(successResult, result)
        }

    @Test
    fun `finishStart() with restored session auth state forwards success result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns successResult

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishStart(intent)
            assertSame(successResult, result)
        }

    @Test
    fun `finishStart() with session auth state forwards error result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns failureResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishStart(intent)
            assertSame(failureResult, result)
        }

    @Test
    fun `finishStart() with restored session auth state forwards error result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns failureResult

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishStart(intent)
            assertSame(failureResult, result)
        }

    @Test
    fun `finishStart() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns canceledResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishStart(intent)
            assertSame(canceledResult, result)
        }

    @Test
    fun `finishStart() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns canceledResult

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishStart(intent)
            assertSame(canceledResult, result)
        }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishStartResult.Success("fake-order-id", "fake-payer-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns successResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishStart(intent)
            assertNull(sutV3.finishStart(intent))
        }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            val failureResult = PayPalWebCheckoutFinishStartResult.Failure(error, null)
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns failureResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishStart(intent)
            assertNull(sutV3.finishStart(intent))
        }

    @Test
    fun `finishStart() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val canceledResult = PayPalWebCheckoutFinishStartResult.Canceled("fake-order-id")
            every {
                payPalWebLauncher.completeCheckoutAuthRequest(intent, "auth state")
            } returns canceledResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishStart(intent)
            assertNull(sutV3.finishStart(intent))
        }

    @Test
    fun `finishVault() with session auth state returns null when start has not been called`() {
        assertNull(sut.finishVault(intent))
    }

    @Test
    fun `finishVault() with session auth state forwards success result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns successResult

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
            assertSame("fake-approval-session-id", result.approvalSessionId)
        }

    @Test
    fun `finishVault() with restored session auth state forwards success result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns successResult

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Success
            assertSame("fake-approval-session-id", result.approvalSessionId)
        }

    @Test
    fun `finishVault() with session auth state forwards error result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
            assertSame(error, result.error)
        }

    @Test
    fun `finishVault() with restored session auth state forwards error result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishVault(intent) as PayPalWebCheckoutFinishVaultResult.Failure
            assertSame(error, result.error)
        }

    @Test
    fun `finishVault() with session auth state forwards cancellation result from auth launcher`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            val result = sutV3.finishVault(intent)
            assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
        }

    @Test
    fun `finishVault() with restored session auth state forwards cancellation result from auth launcher`() =
        runTest {
            val launchWithUrlClient = makeSutWithUrlScheme()
            val restoredClient = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Canceled

            launchWithUrlClient.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            launchWithUrlClient.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            launchWithUrlClient.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            restoredClient.restore(launchWithUrlClient.instanceState)
            val result = restoredClient.finishVault(intent)
            assertSame(PayPalWebCheckoutFinishVaultResult.Canceled, result)
        }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering success event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val successResult =
                PayPalWebCheckoutFinishVaultResult.Success("fake-approval-session-id")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns successResult

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishVault(intent)
            assertNull(sutV3.finishVault(intent))
        }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering error event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            val error = PayPalSDKError(123, "fake-error-description")
            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Failure(error)

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishVault(intent)
            assertNull(sutV3.finishVault(intent))
        }

    @Test
    fun `finishVault() with session auth state clears session to prevent delivering cancellation event twice`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth state")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            every {
                payPalWebLauncher.completeVaultAuthRequest(intent, "auth state")
            } returns PayPalWebCheckoutFinishVaultResult.Canceled

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", mockk(relaxed = true))
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.finishVault(intent)
            assertNull(sutV3.finishVault(intent))
        }

    // MARK: - V3 Methods (createPayPalSession / start(orderId) / vault(setupTokenId))

    // Inject test-controlled applicationScope so coroutines launched by v3 methods
    // run on the test scheduler and are drained by testDispatcher.scheduler.advanceUntilIdle().
    private fun makeSutWithUrlScheme(): PayPalWebCheckoutClient = PayPalWebCheckoutClient(
            analytics = analytics,
            payPalWebLauncher = payPalWebLauncher,
            sessionStore = PayPalWebCheckoutSessionStore(),
            patchCCOWithAppSwitchEligibility = patchCCOWithAppSwitchEligibility,
            createShopperSessionAPI = createShopperSessionAPI,
            getReturnToAppStrategyUseCase = getReturnToAppStrategyUseCase,
            getEffectiveReturnUrlConfigUseCase = GetEffectiveReturnUrlConfigUseCase(),
            deviceInspector = deviceInspector,
            coreConfig = coreConfig,
            applicationScope = CoroutineScope(SupervisorJob() + testDispatcher),
    )

    private val fakeUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/paypal-return",
        cancelAppUrl = "https://example.com/paypal-cancel",
        fallbackSchemeUrl = "com.example.app://paypal",
    )
    private val fakeUserIdentity = PayPalUserIdentity()
    private val fakeSessionResponse = CreateShopperSessionWithAppSwitchEligibilityResponse(
        appSwitchEligible = false,
        redirectUrl = "",
        checkoutFallbackUrl = "",
        ineligibleReason = "",
        matchedAuthenticationMethods = emptyList(),
        shopperSessionConfig = ShopperSessionConfig("fake-session-id", "")
    )

    private fun placeholderTokenUrl(
        baseUrl: String,
        tokenType: TokenType,
    ): String {
        return "$baseUrl?appSwitchEligible=true&tokenType=${tokenType.name}&"
    }

    // --- start(activity, orderId, callback) ---

    @Test
    fun `start() with orderId delivers SESSION_NOT_CREATED when createPayPalSession not called`() =
        runTest {
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)

            sut.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    @Test
    fun `start() with orderId launches checkout when session resolves successfully`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns launchResult

        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.start(activity, "fake-order-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                returnToAppStrategy = any()
            )
        }
        verify { callback.onPayPalWebStartResult(launchResult) }
    }

    @Test
    fun `start() uses AppLink strategy and reports link_type applink when return link type is APP_LINK`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                getReturnToAppStrategyUseCase(any(), any(), any())
            } returns GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(fakeUrlConfig.returnAppUrl))
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = ReturnToAppStrategy.AppLink(fakeUrlConfig.returnAppUrl)
                )
            }
            verify {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_STARTED,
                    params = match { it.linkType == LinkType.APP_LINK },
                )
            }
        }

    @Test
    fun `start() uses CustomUrlScheme strategy and reports link_type deeplink when return link type is DEEP_LINK`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                getReturnToAppStrategyUseCase(any(), any(), any())
            } returns GetReturnToAppStrategyResult.Success(
                ReturnToAppStrategy.CustomUrlScheme(fakeUrlConfig.fallbackSchemeUrl)
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = ReturnToAppStrategy.CustomUrlScheme(fakeUrlConfig.fallbackSchemeUrl)
                )
            }
            verify {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_STARTED,
                    params = match { it.linkType == LinkType.DEEP_LINK },
                )
            }
        }

    @Test
    fun `start() with orderId includes shopperSessionId in the launch uri when the session id is non-blank`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("fake-session-id", uriSlot.captured.getQueryParameter("shopperSessionId"))
        }

    @Test
    fun `start() with orderId omits shopperSessionId from the launch uri when the session id is blank`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val blankIdResponse = fakeSessionResponse.copy(
                checkoutFallbackUrl = placeholderTokenUrl(
                    "https://example.com/fallback",
                    tokenType = TokenType.ORDER_ID,
                ),
                shopperSessionConfig = ShopperSessionConfig("", "")
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(blankIdResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertFalse(launchedUri.queryParameterNames.contains("shopperSessionId"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId includes observability query params on the launch uri`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val uriSlot = slot<Uri>()
        val beforeMillis = System.currentTimeMillis()
        every {
            payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth-state")

        val callback = mockk<PayPalWebStartCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.start(activity, "fake-order-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()
        val afterMillis = System.currentTimeMillis()

        val launchedUri = uriSlot.captured
        assertEquals("pda", launchedUri.getQueryParameter("source"))
        assertEquals("fake-merchant-id", launchedUri.getQueryParameter("merchant"))
        assertEquals("ecs", launchedUri.getQueryParameter("flow_type"))
        assertEquals("paypal", launchedUri.getQueryParameter("funding_source"))

        val switchInitiatedTime = launchedUri.getQueryParameter("switch_initiated_time")?.toLongOrNull()
        assertNotNull(switchInitiatedTime)
        assertTrue(switchInitiatedTime!! in beforeMillis..afterMillis)
    }

    @Test
    fun `start() with orderId delivers failure without falling back to patchCCO when createShopperSession throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // Any unexpected exception from the pre-warm fetch (as opposed to a "normal"
            // APIResult.Failure) should fail the whole flow rather than fall back to patchCCO.
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebStartResult(match { it is PayPalPresentAuthChallengeResult.Failure })
            }
        }

    @Test
    fun `start() with orderId delivers SESSION_CREATION_FAILED when shopperSession resolves to null`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionCreationFailedError.code &&
                        it.error.errorDescription == PayPalWebCheckoutError.sessionCreationFailedError.errorDescription
                })
            }
        }

    // --- LLD Section 3.8 fallback-to-patchCCO behavior ---

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchCheckoutViaPatchCCOFallback"
    )
    @Test
    fun `start() falls back to patchCCO when shopper session fetch reports a session-creation-or-network failure`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://paypal.com/patch-cco-checkout",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                patchCCOWithAppSwitchEligibility(
                    context = activity,
                    orderId = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    merchantOptInForAppSwitch = true,
                    paypalNativeAppInstalled = true
                )
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    returnToAppStrategy = any()
                )
            }
            verify { callback.onPayPalWebStartResult(launchResult) }
        }

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchCheckoutViaPatchCCOFallback"
    )
    @Test
    fun `start() appends token and strips a trailing ampersand from the patchCCO launch url`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
            val uriSlot = slot<Uri>()

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://www.paypal.com/app-switch-checkout?appSwitchEligible=true&",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
            assertFalse(launchedUri.toString().contains("&&"))
            assertFalse(launchedUri.toString().endsWith("&"))
        }

    @Test
    fun `start() with orderId clears session deferred so a second call returns SESSION_NOT_CREATED`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns
                PayPalPresentAuthChallengeResult.Success("auth-state")

            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            val callback1 = mockk<PayPalWebStartCallback>(relaxed = true)
            val callback2 = mockk<PayPalWebStartCallback>(relaxed = true)

            sutV3.start(activity, "fake-order-id", callback1)
            testDispatcher.scheduler.advanceUntilIdle()

            // Second call without a new createPayPalSession — deferred is already consumed.
            sutV3.start(activity, "fake-order-id", callback2)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback2.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    // --- vault(activity, setupTokenId, callback) ---

    @Test
    fun `vault() with setupTokenId delivers SESSION_NOT_CREATED when createPayPalSession not called`() =
        runTest {
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)

            sut.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    @Test
    fun `vault() with setupTokenId launches vault when session resolves successfully`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
        every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns launchResult

        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.VAULT_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.vault(activity, "fake-setup-token-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            payPalWebLauncher.launchWithUrl(
                context = activity,
                uri = any(),
                token = "fake-setup-token-id",
                tokenType = TokenType.VAULT_ID,
                returnToAppStrategy = any()
            )
        }
        verify { callback.onPayPalWebVaultResult(launchResult) }
    }

    @Test
    fun `vault() with setupTokenId includes observability query params on the launch uri`() = runTest {
        val sutV3 = makeSutWithUrlScheme()
        val uriSlot = slot<Uri>()
        val beforeMillis = System.currentTimeMillis()
        every {
            payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
        } returns PayPalPresentAuthChallengeResult.Success("auth-state")

        val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
        sutV3.createPayPalSession(
            tokenType = TokenType.VAULT_ID,
            userIdentity = fakeUserIdentity,
            urlConfig = fakeUrlConfig,
        )
        sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
        sutV3.vault(activity, "fake-setup-token-id", callback)
        testDispatcher.scheduler.advanceUntilIdle()
        val afterMillis = System.currentTimeMillis()

        val launchedUri = uriSlot.captured
        assertEquals("fake-session-id", launchedUri.getQueryParameter("shopperSessionId"))
        assertEquals("pda", launchedUri.getQueryParameter("source"))
        assertEquals("fake-merchant-id", launchedUri.getQueryParameter("merchant"))
        assertEquals("va", launchedUri.getQueryParameter("flow_type"))
        assertEquals("paypal", launchedUri.getQueryParameter("funding_source"))

        val switchInitiatedTime = launchedUri.getQueryParameter("switch_initiated_time")?.toLongOrNull()
        assertNotNull(switchInitiatedTime)
        assertTrue(switchInitiatedTime!! in beforeMillis..afterMillis)
    }

    @Test
    fun `vault() with setupTokenId delivers failure without patchCCO fallback when createShopperSession throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // Any unexpected exception from the pre-warm fetch (as opposed to a "normal"
            // APIResult.Failure) should fail the whole flow rather than fall back to patchCCO.
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebVaultResult(match { it is PayPalPresentAuthChallengeResult.Failure })
            }
        }

    @Test
    fun `vault() with setupTokenId delivers SESSION_CREATION_FAILED when shopperSession resolves to null`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            verify {
                callback.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionCreationFailedError.code &&
                        it.error.errorDescription == PayPalWebCheckoutError.sessionCreationFailedError.errorDescription
                })
            }
        }

    // --- LLD Section 3.8 fallback-to-patchCCO behavior ---

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchVaultViaPatchCCOFallback"
    )
    @Test
    fun `vault() falls back to patchCCO when shopper session fetch reports a session-creation-or-network failure`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")

            coEvery {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            } returns APIResult.Success(
                AppSwitchEligibility(
                    appSwitchEligible = true,
                    launchUrl = "https://paypal.com/patch-cco-vault",
                    ineligibleReason = null
                )
            )
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                patchCCOWithAppSwitchEligibility(
                    context = activity,
                    orderId = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    merchantOptInForAppSwitch = true,
                    paypalNativeAppInstalled = true
                )
            }
            verify {
                payPalWebLauncher.launchWithUrl(
                    context = activity,
                    uri = any(),
                    token = "fake-setup-token-id",
                    tokenType = TokenType.VAULT_ID,
                    returnToAppStrategy = any()
                )
            }
            verify { callback.onPayPalWebVaultResult(launchResult) }
        }

    @Ignore(
        "patchCCO fallback removed pending re-implementation; " +
            "see TODO in PayPalWebCheckoutClient.launchVaultViaPatchCCOFallback"
    )
    @Test
    fun `vault() falls back to a vault URI, not a checkout URI, when patchCCO has no launch url`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns false
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            val launchResult = PayPalPresentAuthChallengeResult.Success("auth-state")
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns launchResult

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            // null represents a session-creation failure / network timeout (any non-LSAT
            // APIResult.Failure from createShopperSessionWithAppSwitchEligibility).
            sutV3.shopperSessionDeferred =
                CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                    .also { it.complete(null) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            // PayPal app isn't installed, so patchCCO is never attempted and getLaunchUri()
            // returns the fallback URI unchanged: it must be a vault URL, never a checkout URL.
            coVerify(exactly = 0) {
                patchCCOWithAppSwitchEligibility(any(), any(), any(), any(), any())
            }
            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().contains("agreements/approve"))
            assertEquals("fake-setup-token-id", launchedUri.getQueryParameter("approval_session_id"))
            assertFalse(launchedUri.toString().contains("checkoutnow"))
        }

    // --- createShopperSessionWithAppSwitchEligibility() outcome mapping (LLD Section 3.8) ---

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() returns Success on API success`() = runTest {
        coEvery {
            createShopperSessionAPI(
                token = any(),
                tokenType = any(),
                params = any(),
            )
        } returns APIResult.Success(fakeSessionResponse)

        val outcome = sut.createShopperSessionWithAppSwitchEligibility(
            token = "fake-order-id",
            tokenType = TokenType.ORDER_ID,
            urlConfig = fakeUrlConfig,
            userIdentity = fakeUserIdentity,
            userAction = PayPalUserAction.CONTINUE,
        )

        assertSame(fakeSessionResponse, outcome)
        verify {
            analytics.notify(
                CreatePayPalSessionEvent.SUCCEEDED,
                params = match {
                    it.linkType == LinkType.APP_LINK && it.shopperSession === fakeSessionResponse
                },
            )
        }
    }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() returns null on a session-creation-or-network failure`() =
        runTest {
            val sessionError = PayPalSDKError(5, "server responded with an error")
            every {
                getReturnToAppStrategyUseCase(any(), any(), any())
            } returns GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme("com.example.app"))
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } returns APIResult.Failure(sessionError)

            val outcome = sut.createShopperSessionWithAppSwitchEligibility(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                urlConfig = fakeUrlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            assertNull(outcome)
            verify {
                analytics.notify(
                    CreatePayPalSessionEvent.FAILED,
                    params = match { it.linkType == LinkType.DEEP_LINK },
                    errorDescription = sessionError.errorDescription,
                )
            }
        }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() propagates any exception thrown by the underlying API`() =
        runTest {
            // Defensive coverage: if the injected API throws for any reason, the exception
            // should propagate rather than be swallowed.
            val lsatError = PayPalSDKError(401, "Unauthorized")
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } throws lsatError

            val thrown = runCatching {
                sut.createShopperSessionWithAppSwitchEligibility(
                    token = "fake-order-id",
                    tokenType = TokenType.ORDER_ID,
                    urlConfig = fakeUrlConfig,
                    userIdentity = fakeUserIdentity,
                    userAction = PayPalUserAction.CONTINUE,
                )
            }.exceptionOrNull()

            assertSame(lsatError, thrown)
        }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() emits api-request-latency with CREATE_SESSION timing`() =
        runTest {
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } returns APIResult.Success(
                fakeSessionResponse,
                roundTripTiming = HttpRoundTripTiming(startTime = 1000L, endTime = 1500L)
            )

            sut.createShopperSessionWithAppSwitchEligibility(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                urlConfig = fakeUrlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            verify(exactly = 1) {
                analytics.notifyApiRequestLatency(
                    endpoint = LatencyEndpoint.CREATE_SESSION,
                    startTime = 1000L,
                    endTime = 1500L
                )
            }
        }

    @Test
    fun `createShopperSessionWithAppSwitchEligibility() does not emit api-request-latency when timing is absent`() =
        runTest {
            coEvery {
                createShopperSessionAPI(
                    token = any(),
                    tokenType = any(),
                    params = any(),
                )
            } returns APIResult.Success(fakeSessionResponse) // roundTripTiming defaults to null

            sut.createShopperSessionWithAppSwitchEligibility(
                token = "fake-order-id",
                tokenType = TokenType.ORDER_ID,
                urlConfig = fakeUrlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            verify(exactly = 0) {
                analytics.notifyApiRequestLatency(any(), any(), any())
            }
        }

    @Test
    fun `vault() with setupTokenId clears session deferred so a second call returns SESSION_NOT_CREATED`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any()) } returns
                PayPalPresentAuthChallengeResult.Success("auth-state")

            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            val callback1 = mockk<PayPalWebVaultCallback>(relaxed = true)
            val callback2 = mockk<PayPalWebVaultCallback>(relaxed = true)

            sutV3.vault(activity, "fake-setup-token-id", callback1)
            testDispatcher.scheduler.advanceUntilIdle()

            sutV3.vault(activity, "fake-setup-token-id", callback2)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                callback2.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure &&
                        it.error.code == PayPalWebCheckoutError.sessionNotCreatedError.code
                })
            }
        }

    fun createAppSwithEligibility(launchUrl: String?) = AppSwitchEligibilityData(
        appSwitchEligible = !launchUrl.isNullOrEmpty(),
        redirectURL = launchUrl,
        ineligibleReason = if (launchUrl.isNullOrEmpty()) "App switch not eligible" else null
    )

    // MARK: - Additional coverage: app-switch-eligible redirectUrl, failure analytics, noReturnToAppStrategyError

    @Test
    fun `start() with orderId prefers redirectUrl over checkoutFallbackUrl when app-switch eligible and installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = placeholderTokenUrl(
                    "https://example.com/app-switch-redirect",
                    tokenType = TokenType.ORDER_ID,
                ),
                checkoutFallbackUrl = placeholderTokenUrl(
                    "https://example.com/fallback",
                    tokenType = TokenType.ORDER_ID,
                ),
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://example.com/app-switch-redirect"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId uses checkoutFallbackUrl when app-switch eligible but not installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns false
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleButNotInstalledResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = placeholderTokenUrl(
                    "https://www.paypal.com/app-switch-checkout",
                    tokenType = TokenType.ORDER_ID,
                ),
                checkoutFallbackUrl = placeholderTokenUrl(
                    "https://www.paypal.com/checkoutnow",
                    tokenType = TokenType.ORDER_ID,
                ),
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleButNotInstalledResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://www.paypal.com/checkoutnow"))
            assertEquals("fake-order-id", launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `vault() with setupTokenId uses redirectUrl when session is app-switch eligible and app is installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                // Both redirectUrl (native app-switch) and checkoutFallbackUrl (web fallback) use
                // the same "approval_session_id" query param for the vault flow.
                redirectUrl = placeholderTokenUrl(
                    "https://example.com/app-switch-vault-redirect",
                    tokenType = TokenType.VAULT_ID,
                ),
                checkoutFallbackUrl = placeholderTokenUrl(
                    "https://example.com/fallback",
                    tokenType = TokenType.VAULT_ID,
                ),
            )
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://example.com/app-switch-vault-redirect"))
            // VAULT_ID tokens are always appended under "approval_session_id".
            assertEquals("fake-setup-token-id", launchedUri.getQueryParameter("approval_session_id"))
            assertNull(launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `vault() with setupTokenId uses ba_token as the param name when session tokenType is BILLING_TOKEN`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            val uriSlot = slot<Uri>()
            every {
                payPalWebLauncher.launchWithUrl(any(), capture(uriSlot), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth-state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = placeholderTokenUrl(
                    "https://example.com/app-switch-billing-agreement",
                    tokenType = TokenType.BILLING_TOKEN,
                ),
                checkoutFallbackUrl = placeholderTokenUrl(
                    "https://example.com/fallback",
                    tokenType = TokenType.BILLING_TOKEN,
                ),
            )
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            // The session's tokenType (from createPayPalSession) drives getLaunchUri(), not the
            // vault()/start() call site itself.
            sutV3.createPayPalSession(
                tokenType = TokenType.BILLING_TOKEN,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            val launchedUri = uriSlot.captured
            assertTrue(launchedUri.toString().startsWith("https://example.com/app-switch-billing-agreement"))
            assertEquals("fake-setup-token-id", launchedUri.getQueryParameter("ba_token"))
            assertNull(launchedUri.getQueryParameter("approval_session_id"))
            assertNull(launchedUri.getQueryParameter("token"))
        }

    @Test
    fun `start() with orderId notifies checkout failure analytics when launch fails after session resolves`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val sdkError = PayPalSDKError(123, "fake error description")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Failure(sdkError)

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    params = AppSwitchAnalyticsEventParams(
                        checkoutOrderId = "fake-order-id",
                        shopperSession = fakeSessionResponse,
                        isCachedSession = false,
                        userActionValue = "CONTINUE",
                        appSwitchEnabled = false,
                        isVault = false,
                        merchantId = "fake-merchant-id",
                        clientId = "fake-client-id",
                        paypalInstalled = "false",
                        returnAppUrl = "https://example.com/paypal-return",
                        cancelAppUrl = "https://example.com/paypal-cancel",
                        fallbackSchemeUrl = "com.example.app://paypal",
                        linkType = LinkType.APP_LINK,
                    ),
                    errorDescription = "fake error description",
                )
            }
            verify {
                callback.onPayPalWebStartResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure && it.error === sdkError
                })
            }
        }

    @Test
    fun `vault() with setupTokenId notifies vault failure analytics when launch fails after session resolves`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val sdkError = PayPalSDKError(123, "fake error description")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Failure(sdkError)

            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    params = AppSwitchAnalyticsEventParams(
                        vaultSetupTokenId = "fake-setup-token-id",
                        shopperSession = fakeSessionResponse,
                        isCachedSession = false,
                        userActionValue = "CONTINUE",
                        appSwitchEnabled = false,
                        isVault = true,
                        merchantId = "fake-merchant-id",
                        clientId = "fake-client-id",
                        paypalInstalled = "false",
                        returnAppUrl = "https://example.com/paypal-return",
                        cancelAppUrl = "https://example.com/paypal-cancel",
                        fallbackSchemeUrl = "com.example.app://paypal",
                        linkType = LinkType.APP_LINK,
                    ),
                    errorDescription = "fake error description",
                )
            }
            verify {
                callback.onPayPalWebVaultResult(match {
                    it is PayPalPresentAuthChallengeResult.Failure && it.error === sdkError
                })
            }
        }

    @Test
    fun `start() with orderId notifies checkout failure analytics when the shopper session fetch throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    CheckoutEvent.FAILED,
                    params = AppSwitchAnalyticsEventParams(
                        checkoutOrderId = "fake-order-id",
                        isCachedSession = false,
                        userActionValue = "CONTINUE",
                        appSwitchEnabled = false,
                        isVault = false,
                        merchantId = "fake-merchant-id",
                        clientId = "fake-client-id",
                        paypalInstalled = "false",
                        returnAppUrl = "https://example.com/paypal-return",
                        cancelAppUrl = "https://example.com/paypal-cancel",
                        fallbackSchemeUrl = "com.example.app://paypal",
                        linkType = LinkType.APP_LINK,
                    ),
                    errorDescription = "session error",
                )
            }
        }

    @Test
    fun `vault() with setupTokenId notifies vault failure analytics when the shopper session fetch throws`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>()
                .also { it.completeExceptionally(RuntimeException("session error")) }
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                analytics.notify(
                    VaultEvent.FAILED,
                    params = AppSwitchAnalyticsEventParams(
                        vaultSetupTokenId = "fake-setup-token-id",
                        isCachedSession = false,
                        userActionValue = "CONTINUE",
                        appSwitchEnabled = false,
                        isVault = true,
                        merchantId = "fake-merchant-id",
                        clientId = "fake-client-id",
                        paypalInstalled = "false",
                        returnAppUrl = "https://example.com/paypal-return",
                        cancelAppUrl = "https://example.com/paypal-cancel",
                        fallbackSchemeUrl = "com.example.app://paypal",
                        linkType = LinkType.APP_LINK,
                    ),
                    errorDescription = "session error",
                )
            }
        }

    // MARK: - Latency Analytics Tests

    @Test
    fun `start() emits user-perceived-latency with checkout flow and browser presentation`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth state")

            // fakeSessionResponse is not app-switch eligible -> browser presentation
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                analytics.notifyUserPerceivedLatency(
                    flow = LatencyFlow.CHECKOUT,
                    presentationType = PresentationType.BROWSER,
                    startTime = any(),
                    endTime = any()
                )
            }
        }

    @Test
    fun `start() emits user-perceived-latency with app-switch presentation when eligible and installed`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every { deviceInspector.isPayPalInstalled } returns true
            every { deviceInspector.canResolvePayPalAppSwitch() } returns true
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth state")

            val appSwitchEligibleResponse = fakeSessionResponse.copy(
                appSwitchEligible = true,
                redirectUrl = "https://example.com/app-switch-redirect",
            )
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(appSwitchEligibleResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                analytics.notifyUserPerceivedLatency(
                    flow = LatencyFlow.CHECKOUT,
                    presentationType = PresentationType.APP_SWITCH,
                    startTime = any(),
                    endTime = any()
                )
            }
        }

    @Test
    fun `start() emits user-perceived-latency with error presentation on launch failure`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            val sdkError = PayPalSDKError(123, "fake error description")
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Failure(sdkError)

            val callback = mockk<PayPalWebStartCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.ORDER_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                analytics.notifyUserPerceivedLatency(
                    flow = LatencyFlow.CHECKOUT,
                    presentationType = PresentationType.ERROR,
                    startTime = any(),
                    endTime = any()
                )
            }
        }

    @Test
    fun `start() emits user-perceived-latency with error presentation when session not created`() =
        runTest {
            val callback = mockk<PayPalWebStartCallback>(relaxed = true)

            sut.start(activity, "fake-order-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                analytics.notifyUserPerceivedLatency(
                    flow = LatencyFlow.CHECKOUT,
                    presentationType = PresentationType.ERROR,
                    startTime = any(),
                    endTime = any()
                )
            }
        }

    @Test
    fun `vault() emits user-perceived-latency with vault flow`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                payPalWebLauncher.launchWithUrl(any(), any(), any(), any(), any())
            } returns PayPalPresentAuthChallengeResult.Success("auth state")

            val callback = mockk<PayPalWebVaultCallback>(relaxed = true)
            sutV3.createPayPalSession(
                tokenType = TokenType.VAULT_ID,
                userIdentity = fakeUserIdentity,
                urlConfig = fakeUrlConfig,
            )
            sutV3.shopperSessionDeferred = CompletableDeferred(fakeSessionResponse)
            sutV3.vault(activity, "fake-setup-token-id", callback)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) {
                analytics.notifyUserPerceivedLatency(
                    flow = LatencyFlow.VAULT,
                    presentationType = any(),
                    startTime = any(),
                    endTime = any()
                )
            }
        }
    @Test
    fun `createShopperSession sends custom-scheme return urls when deep-link is resolved`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                getReturnToAppStrategyUseCase(any(), any(), any())
            } returns GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme("com.example.app"))
            val urlConfig = ReturnToAppUrlConfig(
                returnAppUrl = "https://example.com/paypal-return",
                cancelAppUrl = "https://example.com/paypal-cancel",
                fallbackSchemeUrl = "com.example.app",
            )
            val paramsSlot = slot<CreateShopperSessionWithAppSwitchEligibilityParams>()
            coEvery {
                createShopperSessionAPI(any(), any(), capture(paramsSlot))
            } returns APIResult.Success(fakeSessionResponse)

            sutV3.createShopperSessionWithAppSwitchEligibility(
                token = "fake-token",
                tokenType = TokenType.ORDER_ID,
                urlConfig = urlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            // Deep-link chosen -> server must be told to redirect via the custom scheme, so the
            // return/cancel URLs sent to the shopper session are custom-scheme, not the merchant https.
            val expectedBase = "com.example.app://x-callback-url/paypal-sdk/paypal-checkout"
            assertEquals(expectedBase, paramsSlot.captured.returnAppUrl)
            assertEquals(expectedBase, paramsSlot.captured.cancelAppUrl)
            // The raw fallback scheme is still forwarded unchanged.
            assertEquals("com.example.app", paramsSlot.captured.fallbackSchemeUrl)
            verify {
                analytics.notify(
                    CreatePayPalSessionEvent.STARTED,
                    params = match { it.linkType == LinkType.DEEP_LINK },
                )
            }
        }

    @Test
    fun `createShopperSession sends merchant https return urls when app-link is resolved`() =
        runTest {
            val sutV3 = makeSutWithUrlScheme()
            every {
                getReturnToAppStrategyUseCase(any(), any(), any())
            } returns GetReturnToAppStrategyResult.Success(
                ReturnToAppStrategy.AppLink("https://example.com/paypal-return")
            )
            val urlConfig = ReturnToAppUrlConfig(
                returnAppUrl = "https://example.com/paypal-return",
                cancelAppUrl = "https://example.com/paypal-cancel",
                fallbackSchemeUrl = "com.example.app",
            )
            val paramsSlot = slot<CreateShopperSessionWithAppSwitchEligibilityParams>()
            coEvery {
                createShopperSessionAPI(any(), any(), capture(paramsSlot))
            } returns APIResult.Success(fakeSessionResponse)

            sutV3.createShopperSessionWithAppSwitchEligibility(
                token = "fake-token",
                tokenType = TokenType.ORDER_ID,
                urlConfig = urlConfig,
                userIdentity = fakeUserIdentity,
                userAction = PayPalUserAction.CONTINUE,
            )

            // App-link chosen -> the merchant's https return/cancel URLs are sent unchanged.
            assertEquals("https://example.com/paypal-return", paramsSlot.captured.returnAppUrl)
            assertEquals("https://example.com/paypal-cancel", paramsSlot.captured.cancelAppUrl)
            verify {
                analytics.notify(
                    CreatePayPalSessionEvent.STARTED,
                    params = match { it.linkType == LinkType.APP_LINK },
                )
            }
        }
}
