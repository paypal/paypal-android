package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.paypal.android.executeShellCommandWithOutput

/**
 * Provides API for device-related operations such as app links configuration
 */
class DeviceSettingsRobot {

    companion object {
        private const val TAG = "DeviceSettingsRobot"
    }

    private val chromeRobot = ChromeRobot()

    /**
     * Clears browser cache to force login on next checkout
     * Delegates to ChromeRobot for Chrome-specific cache clearing
     */
    fun clearBrowserCache() {
        Log.d(TAG, "🧹 Clearing browser cache...")
        chromeRobot.clearCache()
    }

    /**
     * Resets all device configurations to default state
     * Should be called in tearDown to ensure test isolation
     */
    fun resetAppLinksToDefaults() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val appPackageName = context.packageName.removeSuffix(".test")

        Log.d(TAG, "🔄 Resetting app links to defaults...")

        executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links --package $appPackageName 0 all"
        )

        Log.d(TAG, "Reset app links complete")
    }

    fun setupAppLinksForCurrentApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val packageName = context.packageName

        // Extract the actual app package name (remove .test suffix for the main app)
        val appPackageName = packageName.removeSuffix(".test")

        Log.d(TAG, "🔗 Setting up app links for return-to-app strategy...")

        // Set app links
        executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links --package $appPackageName 1 all"
        )

        // Set user selection
        executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links-user-selection --package $appPackageName true all"
        )
    }
}
