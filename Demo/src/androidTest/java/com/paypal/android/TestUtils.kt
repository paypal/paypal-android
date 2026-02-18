package com.paypal.android

import android.app.Instrumentation
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.BufferedReader
import java.io.InputStreamReader

fun executeShellCommandWithOutput(
    instrumentation: Instrumentation,
    command: String
): String {
    val output = StringBuilder()
    try {
        val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        val inputStream = java.io.FileInputStream(parcelFileDescriptor.fileDescriptor)
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }
        parcelFileDescriptor.close()
    } catch (e: Exception) {
        output.append("ERROR: ${e.message}")
    }
    return output.toString().trim()
}

/**
 * Dismisses Chrome's first-time setup dialog if present.
 * This dialog appears when Chrome/WebView is launched for the first time on a fresh emulator.
 * The dialog prompts "Make Chrome your own" with options to add account or use without account.
 *
 * @param timeout Timeout in milliseconds (default 5 seconds)
 * @return true if dialog was found and dismissed, false if dialog not present
 */
fun dismissChromeFirstTimeSetup(timeout: Long = 5_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "ChromeSetupUtils"

    Log.d(tag, "🔍 Checking for Chrome first-time setup dialog...")

    // Look for "Make Chrome your own" text or similar first-time setup indicators
    val setupDialog = device.wait(
        Until.findObject(By.textContains("Make Chrome your own")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("Sign in to get your bookmarks")),
        timeout
    )

    if (setupDialog != null) {
        Log.d(tag, "✅ Found Chrome first-time setup dialog, dismissing it")

        // Look for "Use without an account" button
        val useWithoutAccountButton = device.findObject(By.text("Use without an account"))
            ?: device.findObject(By.textContains("without an account"))
            ?: device.findObject(By.textContains("No thanks"))
            ?: device.findObject(By.text("Skip"))

        if (useWithoutAccountButton != null) {
            Log.d(tag, "✅ Clicking 'Use without an account' button")
            useWithoutAccountButton.click()
            Thread.sleep(1000) // Wait for dialog to dismiss
            Log.d(tag, "✅ Chrome first-time setup dismissed successfully")
            return true
        } else {
            Log.w(tag, "⚠️ Could not find 'Use without an account' button")
            // Try pressing back button as fallback
            device.pressBack()
            Thread.sleep(500)
            return false
        }
    } else {
        Log.d(tag, "ℹ️ Chrome first-time setup dialog not present")
        return false
    }
}

/**
 * Clears Chrome cache to force PayPal login
 * This is useful for testing login flows by removing cached sessions
 *
 * @return true if cache was cleared successfully, false otherwise
 */
fun clearChromeCache(): Boolean {
    val tag = "ChromeCacheUtils"
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    Log.d(tag, "🧹 Clearing Chrome cache to force PayPal login...")

    try {
        // Clear Chrome app data
        val result = executeShellCommandWithOutput(
            instrumentation,
            "pm clear com.android.chrome"
        )

        Log.d(tag, "Chrome cache clear result: $result")

        if (result.contains("Success", ignoreCase = true)) {
            Log.d(tag, "✅ Chrome cache cleared successfully")
            Thread.sleep(1000) // Wait for cache clear to complete
            return true
        } else {
            Log.w(tag, "⚠️ Chrome cache clear may have failed: $result")
            return false
        }
    } catch (e: Exception) {
        Log.e(tag, "❌ Error clearing Chrome cache: ${e.message}")
        return false
    }
}
