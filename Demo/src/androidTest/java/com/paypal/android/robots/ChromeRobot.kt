package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.paypal.android.executeShellCommandWithOutput
import com.paypal.android.test.TestConstants

/**
 * Provides API for Chrome browser-related operations
 */
class ChromeRobot {

    companion object {
        private const val TAG = "ChromeRobot"
    }

    /**
     * Dismisses Chrome's first-time setup dialog if present
     * This dialog appears when Chrome/WebView is launched for the first time on a fresh emulator
     *
     * @return true if dialog was found and dismissed, false if dialog not present
     */
    fun dismissFirstTimeSetupIfPresent(): Boolean {
        Log.d(TAG, "🔍 Attempting to dismiss Chrome first-time setup dialog...")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Look for "Make Chrome your own" text or similar first-time setup indicators
        val setupDialog = device.wait(
            Until.findObject(By.textContains("Make Chrome your own")),
            TestConstants.TIMEOUT_SHORT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Sign in to get your bookmarks")),
            TestConstants.TIMEOUT_SHORT_MS
        )

        return if (setupDialog != null) {
            Log.d(TAG, "✅ Found Chrome first-time setup dialog, dismissing it")

            // Look for "Use without an account" button
            val useWithoutAccountButton = device.findObject(By.text("Use without an account"))
                ?: device.findObject(By.textContains("without an account"))
                ?: device.findObject(By.textContains("No thanks"))
                ?: device.findObject(By.text("Skip"))

            if (useWithoutAccountButton != null) {
                Log.d(TAG, "✅ Clicking 'Use without an account' button")
                useWithoutAccountButton.click()
                Thread.sleep(TestConstants.TIMEOUT_SHORT_MS)
                Log.d(TAG, "✅ Chrome first-time setup dismissed successfully")
                true
            } else {
                Log.w(TAG, "⚠️ Could not find 'Use without an account' button")
                // Try pressing back button as fallback
                device.pressBack()
                Thread.sleep(TestConstants.TIMEOUT_SHORT_MS)
                false
            }
        } else {
            Log.d(TAG, "ℹ️ Chrome first-time setup dialog not present")
            false
        }
    }

    /**
     * Clears Chrome cache and data to force fresh login
     * Useful for testing login flows where cached sessions need to be cleared
     *
     * @return true if cache was cleared successfully, false otherwise
     */
    fun clearCache(): Boolean {
        Log.d(TAG, "🧹 Clearing Chrome cache...")
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        return try {
            val result = executeShellCommandWithOutput(
                instrumentation,
                "pm clear com.android.chrome"
            )

            Log.d(TAG, "Chrome cache clear result: $result")

            if (result.contains("Success", ignoreCase = true)) {
                Log.d(TAG, "✅ Chrome cache cleared successfully")
                Thread.sleep(TestConstants.TIMEOUT_SHORT_MS)
                true
            } else {
                Log.w(TAG, "⚠️ Chrome cache clear may have failed: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing Chrome cache: ${e.message}")
            false
        }
    }
}
