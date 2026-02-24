package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.paypal.android.executeShellCommandWithOutput

/**
 * Provides API for device-related operations such as app links configuration
 */
class DeviceSettingsRobot {

    companion object {
        private const val TAG = "DeviceRobot"
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

        Log.d(TAG, "🔄 Resetting device configuration to defaults...")
        resetAppLinks(appPackageName)
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

        Log.d(TAG, "🔗 App links setup completed for package: $appPackageName")
    }

    /**
     * Resets app links for the specified package
     * Disables app links handling to ensure test isolation
     *
     * @param appPackageName The package name of the app to reset (without .test suffix)
     */
    fun resetAppLinks(appPackageName: String) {
        val tag = "AppLinksUtils"
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        Log.d(tag, "🧹 Resetting app links for package: $appPackageName")
        val resetResult = executeShellCommandWithOutput(
            instrumentation,
            "pm set-app-links --package $appPackageName 0 all"
        )
        Log.d(tag, "App links reset result: $resetResult")
    }
}
