package com.paypal.android.paypalpayments

import android.content.Context
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSession
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalLaunchCancellationTrackerUnitTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var returnToAppLauncher: PayPalReturnToAppLauncher
    private lateinit var sut: PayPalLaunchCancellationTracker

    @Before
    fun beforeEach() {
        returnToAppLauncher = mockk(relaxed = true)
        sut = PayPalLaunchCancellationTracker(returnToAppLauncher)
    }

    @Test
    fun `session retained after session end is disposed`() {
        val session = mockk<BrowserSwitchSession>(relaxed = true)
        val launch = startTracking()

        sut.markSessionEnded(launch)
        sut.retainSession(launch, session)

        verify(exactly = 1) { session.dispose() }
    }

    @Test
    fun `session retained after launch replacement is disposed`() {
        val session = mockk<BrowserSwitchSession>(relaxed = true)
        val replaced = startTracking(authState = "replaced-auth-state")
        startTracking(authState = "current-auth-state")

        sut.retainSession(replaced, session)

        verify(exactly = 1) { session.dispose() }
    }

    @Test
    fun `conditional clear with replaced auth state preserves current launch`() {
        val session = mockk<BrowserSwitchSession>(relaxed = true)
        val launch = startTracking(authState = "current-auth-state")
        sut.retainSession(launch, session)

        sut.clear("replaced-auth-state")
        sut.markSessionEnded(launch)
        val result = sut.handleReturnToApp("current-auth-state", CHECKOUT_REQUEST_CODE)

        assertTrue(result is PayPalLaunchCancellationTracker.Result.Canceled)
        verify(exactly = 1) { session.dispose() }
    }

    @Test
    fun `request code mismatch does not observe return to app`() {
        val launch = startTracking()
        sut.armCancellationReturn("auth-state")

        val wrongRequest = sut.handleReturnToApp("auth-state", VAULT_REQUEST_CODE)
        sut.markSessionEnded(launch)
        val matchingRequest = sut.handleReturnToApp("auth-state", CHECKOUT_REQUEST_CODE)

        assertSame(PayPalLaunchCancellationTracker.Result.NoResult, wrongRequest)
        assertTrue(matchingRequest is PayPalLaunchCancellationTracker.Result.Canceled)
        verify(exactly = 0) { returnToAppLauncher.launch(any(), any(), any()) }
    }

    @Test
    fun `blank cancel url leaves cancellation for next return`() {
        val launch = startTracking(cancelUrl = "")
        sut.armCancellationReturn("auth-state")

        val observedReturn = sut.handleReturnToApp("auth-state", CHECKOUT_REQUEST_CODE)
        sut.markSessionEnded(launch)
        val terminalReturn = sut.handleReturnToApp("auth-state", CHECKOUT_REQUEST_CODE)

        assertSame(PayPalLaunchCancellationTracker.Result.NoResult, observedReturn)
        assertTrue(terminalReturn is PayPalLaunchCancellationTracker.Result.Canceled)
        verify(exactly = 0) { returnToAppLauncher.launch(any(), any(), any()) }
    }

    @Test
    fun `scheduled cancellation loses ownership after launch replacement`() {
        val isCurrent = slot<() -> Boolean>()
        val launch = startTracking(authState = "first-auth-state")
        sut.armCancellationReturn("first-auth-state")
        sut.handleReturnToApp("first-auth-state", CHECKOUT_REQUEST_CODE)
        sut.markSessionEnded(launch)
        verify {
            returnToAppLauncher.launch(context, "com.example.app://cancel", capture(isCurrent))
        }

        startTracking(authState = "second-auth-state")

        assertFalse(isCurrent.captured())
    }

    private fun startTracking(
        authState: String = "auth-state",
        cancelUrl: String = "com.example.app://cancel"
    ) = sut.startTracking(
        token = "order-id",
        requestCode = CHECKOUT_REQUEST_CODE,
        authState = authState,
        context = context,
        cancelUrl = cancelUrl
    )

    private companion object {
        const val CHECKOUT_REQUEST_CODE = 1
        const val VAULT_REQUEST_CODE = 2
    }
}
