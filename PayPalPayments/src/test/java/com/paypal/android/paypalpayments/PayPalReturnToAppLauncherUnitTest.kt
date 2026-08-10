package com.paypal.android.paypalpayments

import android.content.Intent
import android.os.Looper
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PayPalReturnToAppLauncherUnitTest {

    private val application = RuntimeEnvironment.getApplication()
    private val sut = PayPalReturnToAppLauncher()

    @Test
    fun `launch sends package scoped marked cancel deep link`() {
        sut.launch(application, "https://example.com/cancel") { true }
        shadowOf(Looper.getMainLooper()).idle()

        val intent = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(application.packageName, intent.`package`)
        assertEquals(
            "true",
            intent.data?.getQueryParameter(
                PayPalReturnToAppLauncher.CANCELLATION_QUERY_PARAM
            )
        )
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            intent.flags
        )
    }

    @Test
    fun `launch drops stale cancellation before starting activity`() {
        sut.launch(application, "https://example.com/cancel") { false }
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(shadowOf(application).nextStartedActivity)
    }
}
