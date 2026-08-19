package com.paypal.android.robots

import android.util.Log
import androidx.test.uiautomator.By

/**
 * Provides API for OTP (One-Time Password) page verification and interaction
 */
class OtpPageRobot {

    private val chromeRobot: ChromeRobot = ChromeRobot()

    companion object {
        private const val TAG = "OtpPageRobot"

        val otpFieldSelectors = listOf(
            By.res("otp")
        )

        val submitBtnSelectors = listOf(
            By.res("submit-button"),
            By.text("Submit"),
            By.text("SUBMIT"),
        )
    }

    fun verifyOtpPage(otpCode: String, maxRetries: Int = 3): Boolean {
        repeat(maxRetries) {
            Log.d(TAG, "🔍 Verifying OTP page is displayed (attempt ${it + 1})...")
            val success = when {
                chromeRobot.chromeAPPClosed() -> {
                    Log.d(
                        TAG,
                        "✅ Chrome app closed before entering OTP, 3DS verification not required"
                    )
                    true
                }

                enterOtpCode(otpCode) && clickSubmitButton() -> {
                    Log.d(TAG, "✅ OTP entered and submit clicked successfully")
                    true
                }

                chromeRobot.dismissFirstTimeSetupIfPresent() && enterOtpCode(otpCode) && clickSubmitButton() -> {
                    Log.d(
                        TAG,
                        "✅ OTP entered and submit clicked successfully after dismissing Chrome setup"
                    )
                    true
                }

                else -> {
                    Log.w(
                        TAG,
                        "⚠️ OTP verification did not complete successfully on attempt ${it + 1}, retrying..."
                    )
                    device.waitForIdle()
                    logPageHierarchy()
                    false
                }
            }

            if (success) {
                return true
            }
            device.waitForIdle()
        }

        Log.e(TAG, "❌ OTP verification failed after $maxRetries attempts")
        return false
    }

    private fun clickSubmitButton(): Boolean {
        findElement(submitBtnSelectors)?.click() ?: run {
            Log.e(TAG, "❌ Submit button not found")
            return false
        }
        Log.d(TAG, "✅ Submit button clicked")
        return true
    }

    fun enterOtpCode(otpCode: String): Boolean {
        Log.d(TAG, "🔢 Entering OTP code: $otpCode")

        findElement(otpFieldSelectors)?.clearAndEnterText(otpCode) ?: run {
            Log.e(TAG, "❌ OTP input field not found")
            return false
        }
        Log.d(TAG, "✅ OTP code entered successfully")
        return true
    }
}
