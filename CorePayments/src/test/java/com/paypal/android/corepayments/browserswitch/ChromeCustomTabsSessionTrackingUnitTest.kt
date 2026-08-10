package com.paypal.android.corepayments.browserswitch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChromeCustomTabsSessionTrackingUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `tracked launch binds callbacks and cleans up exactly once`() = runTest {
        val adapter = FakeCustomTabsAdapter()
        val tabShown = mutableListOf<Unit>()
        val sessionEnded = mutableListOf<Unit>()
        val result = ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            { tabShown += Unit },
            { sessionEnded += Unit }
        )

        adapter.navigationCallback?.onNavigationEvent(CustomTabsCallback.TAB_SHOWN, null)
        adapter.engagementCallback?.onSessionEnded(true, Bundle.EMPTY)
        adapter.engagementCallback?.onSessionEnded(true, Bundle.EMPTY)
        result.session?.dispose()

        assertTrue(result.launchResult is LaunchChromeCustomTabResult.Success)
        assertTrue(adapter.launchedSessions.single() === adapter.session)
        assertEquals(1, tabShown.size)
        assertEquals(1, sessionEnded.size)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `tracked launch publishes state after preparation and immediately before adapter launch`() = runTest {
        val adapter = FakeCustomTabsAdapter()
        var publishCount = 0

        ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            {},
            {},
            onBeforeLaunch = {
                assertTrue(adapter.connection != null)
                assertTrue(adapter.launchedSessions.isEmpty())
                publishCount++
            }
        )

        assertEquals(1, publishCount)
        assertEquals(1, adapter.launchedSessions.size)
    }

    @Test
    fun `unsupported engagement tracking falls back unbound and cleans binding`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { engagementAvailable = false }

        val result = ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            {},
            {}
        )

        assertTrue(result.launchResult is LaunchChromeCustomTabResult.Success)
        assertTrue(result.session == null)
        assertEquals(listOf(null), adapter.launchedSessions)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `connection timeout releases binding and falls back unbound`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { connectOnBind = false }
        val result = async {
            ChromeCustomTabsClient(adapter).launchWithSessionTracking(
                context,
                ChromeCustomTabOptions(Uri.parse("https://example.com")),
                {},
                {}
            )
        }

        advanceUntilIdle()

        assertTrue(result.await().session == null)
        assertEquals(listOf(null), adapter.launchedSessions)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `canceled preparation releases binding without launch`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { connectOnBind = false }
        val job = launch {
            ChromeCustomTabsClient(adapter).launchWithSessionTracking(
                context,
                ChromeCustomTabOptions(Uri.parse("https://example.com")),
                {},
                {}
            )
        }
        runCurrent()

        job.cancelAndJoin()

        assertEquals(1, adapter.unbindCount)
        assertTrue(adapter.launchedSessions.isEmpty())
    }

    @Test
    fun `service disconnect releases binding without reporting session end`() = runTest {
        val adapter = FakeCustomTabsAdapter()
        val sessionEnded = mutableListOf<Unit>()
        val result = ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            {},
            { sessionEnded += Unit }
        )

        adapter.connection?.onServiceDisconnected(ComponentName("browser", "service"))
        result.session?.dispose()

        assertEquals(1, adapter.unbindCount)
        assertTrue(sessionEnded.isEmpty())
    }

    @Test
    fun `callbacks after disposal are ignored`() = runTest {
        val adapter = FakeCustomTabsAdapter()
        val tabShown = mutableListOf<Unit>()
        val sessionEnded = mutableListOf<Unit>()
        val result = ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            { tabShown += Unit },
            { sessionEnded += Unit }
        )

        result.session?.dispose()
        adapter.navigationCallback?.onNavigationEvent(CustomTabsCallback.TAB_SHOWN, null)
        adapter.engagementCallback?.onSessionEnded(true, Bundle.EMPTY)

        assertTrue(tabShown.isEmpty())
        assertTrue(sessionEnded.isEmpty())
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `activity not found during tracked launch cleans binding once`() = runTest {
        val adapter = FakeCustomTabsAdapter().apply { activityNotFound = true }

        val result = ChromeCustomTabsClient(adapter).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            {},
            {}
        )

        assertTrue(result.launchResult is LaunchChromeCustomTabResult.ActivityNotFound)
        assertTrue(result.session == null)
        assertEquals(1, adapter.unbindCount)
    }

    @Test
    fun `disposed prepared binding suppresses tracked launch and falls back unbound`() = runTest {
        val adapter = FakeCustomTabsAdapter()
        val binding = SessionBinding(context, mockk(relaxed = true), adapter, {}, {})
        val serviceHandler = mockk<ChromeCustomTabsServiceHandler>()
        binding.dispose()
        coEvery {
            serviceHandler.prepareTrackedTab(any(), any(), any())
        } returns PreparedCustomTab(adapter.session, binding, "com.example.browser")

        val result = ChromeCustomTabsClient(adapter, serviceHandler).launchWithSessionTracking(
            context,
            ChromeCustomTabOptions(Uri.parse("https://example.com")),
            {},
            {}
        )

        assertTrue(result.launchResult is LaunchChromeCustomTabResult.Success)
        assertTrue(result.session == null)
        assertEquals(listOf(null), adapter.launchedSessions)
    }

    @Test
    fun `tab shown callback is not invoked after binding disposal`() {
        val tabShown = mutableListOf<Unit>()
        val binding = SessionBinding(
            context,
            mockk(relaxed = true),
            FakeCustomTabsAdapter(),
            { tabShown += Unit },
            {}
        )

        binding.dispose()
        binding.notifyTabShown()

        assertTrue(tabShown.isEmpty())
    }

    private class FakeCustomTabsAdapter : CustomTabsAdapter {
        val client = mockk<CustomTabsClient>()
        val session = mockk<CustomTabsSession>()
        var engagementAvailable = true
        var connectOnBind = true
        var activityNotFound = false
        var unbindCount = 0
        var connection: CustomTabsServiceConnection? = null
        var navigationCallback: CustomTabsCallback? = null
        var engagementCallback: EngagementSignalsCallback? = null
        val launchedSessions = mutableListOf<CustomTabsSession?>()

        override fun getPackageName(context: Context) = "com.example.browser"

        override fun bind(
            context: Context,
            packageName: String,
            connection: CustomTabsServiceConnection
        ): Boolean {
            this.connection = connection
            if (connectOnBind) {
                connection.onCustomTabsServiceConnected(
                    ComponentName(packageName, "CustomTabsService"),
                    client
                )
            }
            return true
        }

        override fun unbind(context: Context, connection: CustomTabsServiceConnection) {
            unbindCount++
        }

        override fun newSession(
            client: CustomTabsClient,
            callback: CustomTabsCallback
        ): CustomTabsSession {
            navigationCallback = callback
            return session
        }

        override fun isEngagementSignalsApiAvailable(session: CustomTabsSession) = engagementAvailable

        override fun setEngagementSignalsCallback(
            session: CustomTabsSession,
            callback: EngagementSignalsCallback
        ): Boolean {
            engagementCallback = callback
            return true
        }

        override fun launch(
            context: Context,
            uri: Uri,
            session: CustomTabsSession?,
            packageName: String?
        ) {
            if (activityNotFound && session != null) throw ActivityNotFoundException()
            launchedSessions += session
        }
    }
}
