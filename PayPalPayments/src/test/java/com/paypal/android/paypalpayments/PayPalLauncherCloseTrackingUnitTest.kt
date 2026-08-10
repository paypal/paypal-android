package com.paypal.android.paypalpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSession
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSessionStartResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.model.TokenType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PayPalLauncherCloseTrackingUnitTest {

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private lateinit var browserSwitchClient: BrowserSwitchClient
    private lateinit var returnToAppLauncher: PayPalReturnToAppLauncher
    private lateinit var sut: PayPalLauncher

    @Before
    fun beforeEach() {
        browserSwitchClient = mockk(relaxed = true)
        returnToAppLauncher = mockk(relaxed = true)
        sut = PayPalLauncher(browserSwitchClient, returnToAppLauncher)
    }

    @Test
    fun `checkout and vault session end before finish return typed cancellation`() = runTest {
        val sessionEnded = mutableListOf<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()

        val checkout = launchTracked("order-id", TokenType.ORDER_ID)
        sessionEnded.last().invoke()
        val checkoutResult = sut.completeCheckoutAuthRequest(Intent(), checkout.authState)
        val vault = launchTracked("setup-token-id", TokenType.VAULT_ID)
        sessionEnded.last().invoke()
        val vaultResult = sut.completeVaultAuthRequest(Intent(), vault.authState)

        assertEquals(
            "order-id",
            (checkoutResult as PayPalFinishStartResult.Canceled).orderId
        )
        assertSame(PayPalFinishVaultResult.Canceled, vaultResult)
    }

    @Test
    fun `checkout close cannot be consumed or observed by vault finish`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()
        val launch = launchTracked("order-id", TokenType.ORDER_ID)

        val wrongFlow = sut.completeVaultAuthRequest(Intent(), launch.authState)
        sessionEnded.captured.invoke()
        val checkoutResult = sut.completeCheckoutAuthRequest(Intent(), launch.authState)

        assertSame(PayPalFinishVaultResult.NoResult, wrongFlow)
        assertTrue(checkoutResult is PayPalFinishStartResult.Canceled)
    }

    @Test
    fun `vault close cannot be consumed or observed by checkout finish`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()
        val launch = launchTracked("setup-token-id", TokenType.VAULT_ID)

        val wrongFlow = sut.completeCheckoutAuthRequest(Intent(), launch.authState)
        sessionEnded.captured.invoke()
        val vaultResult = sut.completeVaultAuthRequest(Intent(), launch.authState)

        assertSame(PayPalFinishStartResult.NoResult, wrongFlow)
        assertSame(PayPalFinishVaultResult.Canceled, vaultResult)
    }

    @Test
    fun `checkout finish before session end dispatches cancel url once`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()
        val launch = launchTracked("order-id", TokenType.ORDER_ID)

        val result = sut.completeCheckoutAuthRequest(Intent(), launch.authState)
        sessionEnded.captured.invoke()
        sessionEnded.captured.invoke()

        assertSame(PayPalFinishStartResult.NoResult, result)
        verify(exactly = 1) {
            returnToAppLauncher.launch(any(), "com.example.app://cancel", any())
        }
    }

    @Test
    fun `vault finish before session end dispatches cancel url once`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()
        val launch = launchTracked("setup-token-id", TokenType.VAULT_ID)

        val result = sut.completeVaultAuthRequest(Intent(), launch.authState)
        sessionEnded.captured.invoke()
        sessionEnded.captured.invoke()

        assertSame(PayPalFinishVaultResult.NoResult, result)
        verify(exactly = 1) {
            returnToAppLauncher.launch(any(), "com.example.app://cancel", any())
        }
    }

    @Test
    fun `auth state is published before tracked browser launch`() = runTest {
        var publishedAuthState: String? = null
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), any(), any())
        } answers {
            arg<() -> Unit>(4).invoke()
            assertNotNull(publishedAuthState)
            trackedSuccess()
        }

        val result = sut.launchWithUrlAndSessionTracking(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "order-id",
            TokenType.ORDER_ID,
            ReturnToAppStrategy.CustomUrlScheme("com.example.app"),
            "com.example.app://cancel",
            onAuthStateCreated = { publishedAuthState = it }
        ) as PayPalPresentAuthChallengeResult.Success

        assertEquals(result.authState, publishedAuthState)
    }

    @Test
    fun `older launch failure does not clear newer auth state`() = runTest {
        val firstLaunchPublished = CountDownLatch(1)
        val releaseFirstLaunch = CountDownLatch(1)
        var launchCount = 0
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), any(), any())
        } answers {
            arg<() -> Unit>(4).invoke()
            if (launchCount++ == 0) {
                firstLaunchPublished.countDown()
                assertTrue(releaseFirstLaunch.await(5, TimeUnit.SECONDS))
                BrowserSwitchSessionStartResult(
                    BrowserSwitchStartResult.Failure(Exception("first launch failed")),
                    null
                )
            } else {
                trackedSuccess()
            }
        }
        var storedAuthState: String? = null
        val publishAuthState = { authState: String -> storedAuthState = authState }
        val clearAuthState = { authState: String ->
            if (storedAuthState == authState) storedAuthState = null
        }

        val firstResult = async(Dispatchers.Default) {
            sut.launchWithUrlAndSessionTracking(
                activity,
                Uri.parse("https://paypal.com/checkout"),
                "same-order",
                TokenType.ORDER_ID,
                ReturnToAppStrategy.CustomUrlScheme("com.example.app"),
                "com.example.app://cancel",
                publishAuthState,
                clearAuthState
            )
        }
        assertTrue(firstLaunchPublished.await(5, TimeUnit.SECONDS))
        val firstAuthState = storedAuthState
        val secondResult = sut.launchWithUrlAndSessionTracking(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "same-order",
            TokenType.ORDER_ID,
            ReturnToAppStrategy.CustomUrlScheme("com.example.app"),
            "com.example.app://cancel",
            publishAuthState,
            clearAuthState
        ) as PayPalPresentAuthChallengeResult.Success
        releaseFirstLaunch.countDown()

        assertTrue(firstResult.await() is PayPalPresentAuthChallengeResult.Failure)
        assertFalse(firstAuthState == secondResult.authState)
        assertEquals(secondResult.authState, storedAuthState)
    }

    @Test
    fun `valid checkout uri clears scheduled cancellation before dispatch`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        val isCurrent = slot<() -> Boolean>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess()
        val launch = launchTracked("order-id", TokenType.ORDER_ID)
        sut.completeCheckoutAuthRequest(Intent(), launch.authState)
        sessionEnded.captured.invoke()
        verify {
            returnToAppLauncher.launch(any(), "com.example.app://cancel", capture(isCurrent))
        }
        val result = sut.completeCheckoutAuthRequest(
            Intent().setData(Uri.parse("com.example.app://return?PayerID=payer-id")),
            launch.authState
        )

        assertTrue(result is PayPalFinishStartResult.Success)
        assertFalse(isCurrent.captured())
    }

    @Test
    fun `valid checkout uri wins after session end and session cleans once`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        val session = mockk<BrowserSwitchSession>(relaxed = true)
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess(session)
        val launch = launchTracked("order-id", TokenType.ORDER_ID)
        sessionEnded.captured.invoke()
        val intent = Intent().setData(Uri.parse("com.example.app://return?PayerID=payer-id"))

        val result = sut.completeCheckoutAuthRequest(intent, launch.authState)

        assertTrue(result is PayPalFinishStartResult.Success)
        verify(exactly = 1) { session.dispose() }
    }

    @Test
    fun `valid vault uri wins after session end and session cleans once`() = runTest {
        val sessionEnded = slot<() -> Unit>()
        val session = mockk<BrowserSwitchSession>(relaxed = true)
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returns trackedSuccess(session)
        val launch = launchTracked("setup-token-id", TokenType.VAULT_ID)
        sessionEnded.captured.invoke()
        val intent = Intent().setData(
            Uri.parse("com.example.app://return?approval_session_id=approval-id")
        )

        val result = sut.completeVaultAuthRequest(intent, launch.authState)

        assertTrue(result is PayPalFinishVaultResult.Success)
        verify(exactly = 1) { session.dispose() }
    }

    @Test
    fun `tab shown invalidates minimized observer without losing later close`() = runTest {
        val tabShown = slot<() -> Unit>()
        val sessionEnded = slot<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(
                activity,
                any(),
                capture(tabShown),
                capture(sessionEnded),
                any()
            )
        } returns trackedSuccess()
        val launch = launchTracked("order-id", TokenType.ORDER_ID)
        sut.completeCheckoutAuthRequest(Intent(), launch.authState)

        tabShown.captured.invoke()
        sessionEnded.captured.invoke()
        val resumedResult = sut.completeCheckoutAuthRequest(Intent(), launch.authState)

        verify(exactly = 0) { returnToAppLauncher.launch(any(), any(), any()) }
        assertTrue(resumedResult is PayPalFinishStartResult.Canceled)
    }

    @Test
    fun `tab shown invalidates checkout and vault observers before valid uri`() = runTest {
        val tabShown = mutableListOf<() -> Unit>()
        val sessionEnded = mutableListOf<() -> Unit>()
        coEvery {
            browserSwitchClient.startWithSessionTracking(
                activity,
                any(),
                capture(tabShown),
                capture(sessionEnded),
                any()
            )
        } returns trackedSuccess()

        val checkout = launchTracked("order-id", TokenType.ORDER_ID)
        sut.completeCheckoutAuthRequest(Intent(), checkout.authState)
        tabShown.last().invoke()
        val checkoutResult = sut.completeCheckoutAuthRequest(
            Intent().setData(Uri.parse("com.example.app://return?PayerID=payer-id")),
            checkout.authState
        )
        sessionEnded.last().invoke()

        val vault = launchTracked("setup-token-id", TokenType.VAULT_ID)
        sut.completeVaultAuthRequest(Intent(), vault.authState)
        tabShown.last().invoke()
        val vaultResult = sut.completeVaultAuthRequest(
            Intent().setData(Uri.parse("com.example.app://return?approval_session_id=approval-id")),
            vault.authState
        )
        sessionEnded.last().invoke()

        assertTrue(checkoutResult is PayPalFinishStartResult.Success)
        assertTrue(vaultResult is PayPalFinishVaultResult.Success)
        verify(exactly = 0) { returnToAppLauncher.launch(any(), any(), any()) }
    }

    @Test
    fun `launch replacement preserves only current cancellation ownership`() = runTest {
        val sessionEnded = mutableListOf<() -> Unit>()
        val sessions = listOf(
            mockk<BrowserSwitchSession>(relaxed = true),
            mockk<BrowserSwitchSession>(relaxed = true)
        )
        coEvery {
            browserSwitchClient.startWithSessionTracking(activity, any(), any(), capture(sessionEnded), any())
        } returnsMany sessions.map(::trackedSuccess)
        val firstLaunch = launchTracked("first-order", TokenType.ORDER_ID)
        sut.completeCheckoutAuthRequest(Intent(), firstLaunch.authState)
        val secondLaunch = launchTracked("second-order", TokenType.ORDER_ID)
        verify(exactly = 1) { sessions.first().dispose() }

        sessionEnded.first().invoke()
        sut.completeCheckoutAuthRequest(Intent(), secondLaunch.authState)
        sessionEnded.last().invoke()

        verify(exactly = 1) {
            returnToAppLauncher.launch(any(), "com.example.app://cancel", any())
        }
        verify(exactly = 1) { sessions.last().dispose() }
    }

    @Test
    fun `app switch launch remains untracked and ignored intent stays no result`() = runTest {
        every { browserSwitchClient.start(activity, any(), any()) } returns BrowserSwitchStartResult.Success

        val launch = sut.launchWithUrl(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            "order-id",
            TokenType.ORDER_ID,
            ReturnToAppStrategy.CustomUrlScheme("com.example.app")
        ) as PayPalPresentAuthChallengeResult.Success
        val result = sut.completeCheckoutAuthRequest(Intent(), launch.authState)

        assertSame(PayPalFinishStartResult.NoResult, result)
        coVerify(exactly = 0) {
            browserSwitchClient.startWithSessionTracking(any(), any(), any(), any(), any())
        }
    }

    private suspend fun launchTracked(
        token: String,
        tokenType: TokenType
    ): PayPalPresentAuthChallengeResult.Success {
        val result = sut.launchWithUrlAndSessionTracking(
            activity,
            Uri.parse("https://paypal.com/checkout"),
            token,
            tokenType,
            ReturnToAppStrategy.CustomUrlScheme("com.example.app"),
            "com.example.app://cancel"
        ) as PayPalPresentAuthChallengeResult.Success
        return result
    }

    private fun trackedSuccess(session: BrowserSwitchSession? = null) =
        BrowserSwitchSessionStartResult(BrowserSwitchStartResult.Success, session)
}
