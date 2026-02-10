package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Robot for interacting with PayPal web pages (login, checkout) in browser/Chrome Custom Tab.
 * This robot encapsulates all web page interactions using UiAutomator.
 */
class PayPalWebPageRobot {

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    companion object {
        private const val TAG = "PayPalWebPageRobot"

        // Timeout constants
        private const val WEB_TIMEOUT_MS = 30_000L
        private const val PAGE_DETECTION_TIMEOUT_MS = 3_000L
        private const val LOGIN_FLOW_TIMEOUT_MS = 5_000L
        private const val NAVIGATION_TIMEOUT_MS = 10_000L
    }

    /**
     * Performs the complete PayPal login flow.
     * Intelligently handles different scenarios:
     * 1. Already logged in - skips login
     * 2. Not logged in - enters credentials
     * 3. One-time code page - clicks "Try another way" -> "Use password instead"
     * 4. Direct password page - enters password directly
     *
     * @param email The PayPal account email
     * @param password The PayPal account password
     * @return true if login was completed successfully, false otherwise
     */
    fun performLogin(email: String, password: String): Boolean {
        Log.d(TAG, "🔐 Starting PayPal login flow")

        // Wait a moment for page to load
        Thread.sleep(2000)

        // Scenario 1: Check if already on review order page (already logged in)
        if (isOnReviewOrderPage()) {
            Log.d(TAG, "✅ User already logged in, skipping login flow")
            return true
        }

        // Scenario 2: Check if on login page (not logged in)
        if (isOnLoginPage()) {
            Log.d(TAG, "📧 On login page, entering email")
            val emailEntered = enterEmail(email)
            if (!emailEntered) {
                Log.e(TAG, "❌ Failed to enter email")
                return false
            }

            // Wait for next page after email
            Thread.sleep(3000)

            // Scenario 2a: Check if on one-time code page
            if (isOnOneTimeCodePage()) {
                Log.d(TAG, "📱 On one-time code page, clicking 'Try another way'")
                val tryAnotherWayClicked = clickTryAnotherWay()
                if (tryAnotherWayClicked) {
                    Log.d(TAG, "✅ Clicked 'Try another way'")

                    // Wait for modal to appear
                    Thread.sleep(2000)

                    // Click "Use password instead" from the modal
                    val usePasswordClicked = clickUsePasswordInstead()
                    if (usePasswordClicked) {
                        Log.d(TAG, "✅ Clicked 'Use password instead' button")
                    } else {
                        Log.w(TAG, "⚠️ Could not find 'Use password instead' button")
                    }
                } else {
                    Log.w(TAG, "⚠️ Failed to click 'Try another way' button")
                }
            } else if (isOnPasswordPage()) {
                // Scenario 2b: Directly on password page
                Log.d(TAG, "🔑 Directly on password page")
            } else {
                Log.w(TAG, "⚠️ Unexpected page after entering email")
            }

            // Enter password
            val passwordEntered = enterPassword(password)
            if (!passwordEntered) {
                Log.e(TAG, "❌ Failed to enter password")
                return false
            }
        } else {
            Log.w(TAG, "⚠️ Not on login page or review order page - unexpected state")
            return false
        }

        Log.d(TAG, "✅ PayPal login completed successfully")
        return true
    }

    /**
     * Completes the PayPal review order page by clicking appropriate buttons
     */
    fun completeReviewOrder() {
        // Look for common PayPal web elements like "Review Order", "Pay Now", or "Continue"
        val reviewOrderButton = device.wait(
            Until.findObject(By.textContains("Review")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Continue")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Pay Now")),
            WEB_TIMEOUT_MS
        )

        if (reviewOrderButton != null) {
            Log.d(TAG, "✅ Found PayPal checkout button in browser")
            reviewOrderButton.click()
            Thread.sleep(2000) // Wait for click to process
        } else {
            Log.w(
                TAG,
                "⚠️ Could not find PayPal checkout button - may be in app switch or sandbox mode"
            )
        }
    }

    // ========== Page Detection Methods ==========

    private fun isOnReviewOrderPage(): Boolean {
        val reviewOrderIndicators = device.wait(
            Until.findObject(By.textContains("Review")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Continue")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Pay Now")),
            LOGIN_FLOW_TIMEOUT_MS
        )

        val isOnPage = reviewOrderIndicators != null
        if (isOnPage) {
            Log.d(TAG, "✅ Already on review order page - user is logged in")
        }
        return isOnPage
    }

    private fun isOnLoginPage(): Boolean {
        val emailField = device.wait(
            Until.findObject(By.res("email")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.res("login_emailField")),
            LOGIN_FLOW_TIMEOUT_MS
        )

        val isOnPage = emailField != null
        if (isOnPage) {
            Log.d(TAG, "✅ On login page - email field found")
        }
        return isOnPage
    }

    private fun isOnOneTimeCodePage(): Boolean {
        Log.d(TAG, "🔍 Checking if on one-time code page...")

        val oneTimeCodeIndicators = device.wait(
            Until.findObject(By.textContains("one-time code")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Get a Code")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.text("Try another way")),
            LOGIN_FLOW_TIMEOUT_MS
        )

        val isOnPage = oneTimeCodeIndicators != null
        if (isOnPage) {
            Log.d(TAG, "✅ Detected one-time code page")
        } else {
            Log.d(TAG, "ℹ️ Not on one-time code page")
        }
        return isOnPage
    }

    private fun isOnPasswordPage(): Boolean {
        val passwordField = device.wait(
            Until.findObject(By.res("password")),
            LOGIN_FLOW_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.res("login_passwordField")),
            LOGIN_FLOW_TIMEOUT_MS
        )

        val isOnPage = passwordField != null
        if (isOnPage) {
            Log.d(TAG, "✅ On password page")
        }
        return isOnPage
    }

    // ========== Form Input Methods ==========

    private fun enterEmail(email: String): Boolean {
        // Look for email/username input field
        val emailField = device.wait(
            Until.findObject(By.res("email")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.res("login_emailField")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            WEB_TIMEOUT_MS
        )

        if (emailField != null) {
            Log.d(TAG, "✅ Found email field, entering: $email")
            emailField.text = email
            Thread.sleep(1000) // Wait for input to register

            // Look for Next or Continue button
            val nextButton = device.findObject(By.text("Next"))
                ?: device.findObject(By.textContains("Continue"))
                ?: device.findObject(By.res("btnNext"))

            if (nextButton != null) {
                nextButton.click()
                Log.d(TAG, "✅ Clicked Next button")
                Thread.sleep(2000) // Wait for navigation to password page
                return true
            } else {
                Log.w(TAG, "⚠️ Could not find Next/Continue button")
                return false
            }
        } else {
            Log.w(TAG, "⚠️ Could not find email field")
            return false
        }
    }

    private fun enterPassword(password: String): Boolean {
        // Wait for password field
        val passwordField = device.wait(
            Until.findObject(By.res("password")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.res("login_passwordField")),
            WEB_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            WEB_TIMEOUT_MS
        )

        if (passwordField != null) {
            Log.d(TAG, "✅ Found password field, entering password")
            passwordField.text = password
            Thread.sleep(1000) // Wait for input to register

            // Look for Login button
            val loginButton = device.findObject(By.text("Log In"))
                ?: device.findObject(By.textContains("Sign In"))
                ?: device.findObject(By.res("btnLogin"))

            if (loginButton != null) {
                loginButton.click()
                Log.d(TAG, "✅ Clicked Login button")
                Thread.sleep(3000) // Wait for login to process
                return true
            } else {
                Log.w(TAG, "⚠️ Could not find Login/Sign In button")
                return false
            }
        } else {
            Log.w(TAG, "⚠️ Could not find password field")
            return false
        }
    }

    // ========== Navigation Methods ==========

    private fun clickTryAnotherWay(): Boolean {
        Log.d(TAG, "🔍 Looking for 'Try another way' button...")

        // Wait for "Try another way" button to appear
        val tryAnotherWayButton = device.wait(
            Until.findObject(By.text("Try another way")),
            NAVIGATION_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("another way")),
            NAVIGATION_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("different")),
            NAVIGATION_TIMEOUT_MS
        )

        if (tryAnotherWayButton != null) {
            Log.d(TAG, "✅ Found 'Try another way' button, clicking it")
            tryAnotherWayButton.click()
            Thread.sleep(2000) // Wait for navigation
            return true
        } else {
            Log.w(
                TAG,
                "❌ 'Try another way' button not found after waiting ${NAVIGATION_TIMEOUT_MS}ms"
            )
            return false
        }
    }

    private fun clickUsePasswordInstead(): Boolean {
        Log.d(TAG, "🔍 Looking for 'Use password instead' button...")

        // Wait for "Use password instead" button to appear
        val usePasswordButton = device.wait(
            Until.findObject(By.text("Use password instead")),
            NAVIGATION_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("password instead")),
            NAVIGATION_TIMEOUT_MS
        ) ?: device.wait(
            Until.findObject(By.textContains("Use password")),
            NAVIGATION_TIMEOUT_MS
        )

        if (usePasswordButton != null) {
            Log.d(TAG, "✅ Found 'Use password instead' button, clicking it")
            usePasswordButton.click()
            Thread.sleep(2000) // Wait for password page to load
            return true
        } else {
            Log.w(
                TAG,
                "❌ 'Use password instead' button not found after waiting ${NAVIGATION_TIMEOUT_MS}ms"
            )
            return false
        }
    }
}
