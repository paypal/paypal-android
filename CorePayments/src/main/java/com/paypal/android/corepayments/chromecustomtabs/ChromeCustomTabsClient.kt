package com.paypal.android.corepayments.chromecustomtabs

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSession

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ChromeCustomTabsClient internal constructor(
    private val adapter: CustomTabsAdapter = AndroidCustomTabsAdapter(),
    private val serviceHandler: ChromeCustomTabsServiceHandler = ChromeCustomTabsServiceHandler(adapter)
) {

    fun launch(
        context: Context,
        options: ChromeCustomTabOptions,
        onBeforeLaunch: () -> Unit = {}
    ): LaunchChromeCustomTabResult {
        return try {
            onBeforeLaunch()
            adapter.launch(context, options.launchUri, null, null)
            LaunchChromeCustomTabResult.Success
        } catch (_: ActivityNotFoundException) {
            LaunchChromeCustomTabResult.ActivityNotFound
        }
    }

    suspend fun launchWithSessionTracking(
        context: Context,
        options: ChromeCustomTabOptions,
        onTabShown: () -> Unit,
        onSessionEnded: () -> Unit,
        onBeforeLaunch: () -> Unit = {}
    ): TrackedChromeCustomTabResult {
        val preparedTab = serviceHandler.prepareTrackedTab(
            context.applicationContext,
            onTabShown,
            onSessionEnded
        )
        return if (preparedTab == null) {
            TrackedChromeCustomTabResult(launch(context, options, onBeforeLaunch), null)
        } else {
            launchPreparedTab(context, options, preparedTab, onBeforeLaunch)
        }
    }

    private fun launchPreparedTab(
        context: Context,
        options: ChromeCustomTabOptions,
        preparedTab: PreparedCustomTab,
        onBeforeLaunch: () -> Unit
    ): TrackedChromeCustomTabResult = try {
        val didLaunch = preparedTab.binding.runIfNotDisposed {
            onBeforeLaunch()
            adapter.launch(context, options.launchUri, preparedTab.session, preparedTab.packageName)
        }
        if (didLaunch) {
            TrackedChromeCustomTabResult(
                LaunchChromeCustomTabResult.Success,
                BrowserSwitchSession { preparedTab.binding.dispose() }
            )
        } else {
            TrackedChromeCustomTabResult(launch(context, options, onBeforeLaunch), null)
        }
    } catch (_: ActivityNotFoundException) {
        preparedTab.binding.dispose()
        TrackedChromeCustomTabResult(LaunchChromeCustomTabResult.ActivityNotFound, null)
    } catch (_: SecurityException) {
        preparedTab.binding.dispose()
        TrackedChromeCustomTabResult(launch(context, options, onBeforeLaunch), null)
    } catch (_: UnsupportedOperationException) {
        preparedTab.binding.dispose()
        TrackedChromeCustomTabResult(launch(context, options, onBeforeLaunch), null)
    }
}
