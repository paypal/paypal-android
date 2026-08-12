package com.paypal.android.corepayments.chromecustomtabs

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.RemoteException
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChromeCustomTabsServiceHandlerUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `package lookup failure skips service binding`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply {
            packageLookupFailure = SecurityException()
        }

        val result = ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})

        assertNull(result)
        assertEquals(0, adapter.bindCount)
        assertEquals(0, adapter.unbindCount)
    }

    @Test
    fun `failed bind returns null without unbinding`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { bindResult = false }

        val result = ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})

        assertNull(result)
        assertEquals(1, adapter.bindCount)
        assertEquals(0, adapter.unbindCount)
    }

    @Test
    fun `session creation failure releases successful binding`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply {
            newSessionFailure = UnsupportedOperationException()
        }

        val result = ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})

        assertNull(result)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `engagement callback failure releases successful binding`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply {
            engagementCallbackFailure = RemoteException()
        }

        val result = ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})

        assertNull(result)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `service disconnect during preparation completes with cleanup`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { connectOnBind = false }
        val result = async {
            ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})
        }
        runCurrent()

        adapter.connection?.onServiceDisconnected(ComponentName(BROWSER_PACKAGE, SERVICE_NAME))

        assertNull(result.await())
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `late connection after timeout does not create a session or unbind twice`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { connectOnBind = false }
        val result = async {
            ChromeCustomTabsServiceHandler(adapter).prepareTrackedTab(context, {}, {})
        }

        advanceUntilIdle()
        adapter.connection?.onCustomTabsServiceConnected(
            ComponentName(BROWSER_PACKAGE, SERVICE_NAME),
            adapter.client
        )

        assertNull(result.await())
        assertEquals(0, adapter.newSessionCount)
        assertEquals(1, adapter.unbindCount)
    }

    private class FakeCustomTabsAdapter : CustomTabsAdapter {
        val client = mockk<CustomTabsClient>()
        private val session = mockk<CustomTabsSession>()
        var packageLookupFailure: Throwable? = null
        var bindResult = true
        var connectOnBind = true
        var newSessionFailure: Throwable? = null
        var engagementCallbackFailure: Throwable? = null
        var bindCount = 0
        var unbindCount = 0
        var newSessionCount = 0
        var connection: CustomTabsServiceConnection? = null

        override fun getPackageName(context: Context): String? {
            packageLookupFailure?.let { throw it }
            return BROWSER_PACKAGE
        }

        override fun bind(
            context: Context,
            packageName: String,
            connection: CustomTabsServiceConnection
        ): Boolean {
            bindCount++
            this.connection = connection
            if (bindResult && connectOnBind) {
                connection.onCustomTabsServiceConnected(
                    ComponentName(packageName, SERVICE_NAME),
                    client
                )
            }
            return bindResult
        }

        override fun unbind(context: Context, connection: CustomTabsServiceConnection) {
            unbindCount++
        }

        override fun newSession(
            client: CustomTabsClient,
            callback: CustomTabsCallback
        ): CustomTabsSession {
            newSessionCount++
            newSessionFailure?.let { throw it }
            return session
        }

        override fun isEngagementSignalsApiAvailable(session: CustomTabsSession) = true

        override fun setEngagementSignalsCallback(
            session: CustomTabsSession,
            callback: EngagementSignalsCallback
        ): Boolean {
            engagementCallbackFailure?.let { throw it }
            return true
        }

        override fun launch(
            context: Context,
            uri: Uri,
            session: CustomTabsSession?,
            packageName: String?
        ) = Unit
    }

    private companion object {
        const val BROWSER_PACKAGE = "com.example.browser"
        const val SERVICE_NAME = "CustomTabsService"
    }
}
