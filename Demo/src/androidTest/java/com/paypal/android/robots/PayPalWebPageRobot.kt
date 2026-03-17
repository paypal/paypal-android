package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.paypal.android.utils.TestConstants.TIMEOUT_LONG_MS

/**
 * Robot for interacting with PayPal web pages (login, checkout) in browser/Chrome Custom Tab.
 * This robot encapsulates all web page interactions using UiAutomator.
 *
 * Handles multiple login scenarios:
 * - Already logged in (session active)
 * - Direct one-time code page (email remembered from previous session)
 * - Single-page login (email + password on same page)
 * - Two-step login (email -> next -> password)
 * - One-time code redirect after email entry (requires "Try another way" navigation)
 */
class PayPalWebPageRobot {

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val chromeRobot = ChromeRobot()

    companion object {
        private const val TAG = "PayPalWebPageRobot"

        // UI Selectors - Email field
        private val EMAIL_FIELD_SELECTORS = listOf(
            By.res("email"),
            By.res("login_emailField")
        )

        // UI Selectors - Password field
        private val PASSWORD_FIELD_SELECTORS = listOf(
            By.res("password"),
            By.res("login_passwordField")
        )

        // UI Selectors - Buttons
        private val NEXT_BUTTON_SELECTORS = listOf(
            By.text("Next"),
            By.textContains("Continue"),
            By.res("btnNext")
        )

        private val LOGIN_BUTTON_SELECTORS = listOf(
            By.text("Log In"),
            By.textContains("Sign In"),
            By.res("btnLogin")
        )

        private val REVIEW_ORDER_SELECTORS = listOf(
            By.res("one-time-cta"),
            By.textContains("Review"),
            By.textContains("Continue"),
            By.textContains("Pay Now"),
            By.textContains("Agree")
        )

        private val TRY_ANOTHER_WAY_SELECTORS = listOf(
            By.text("Try another way"),
            By.textContains("another way"),
            By.textContains("different")
        )

        private val USE_PASSWORD_SELECTORS = listOf(
            By.text("Use password instead"),
            By.textContains("password instead"),
            By.textContains("Use password")
        )
    }

    /**
     * Performs the complete PayPal login flow.
     * Intelligently detects current page state and performs appropriate actions in a single pass.
     * This merged detection+action approach reduces delays by avoiding separate detection cycles.
     *
     * Handles scenarios:
     * 1. Already logged in - clicks review button and returns
     * 2. Direct one-time code page - navigates to password, enters password
     * 3. Single-page login - enters email and password directly
     * 4. Two-step login - enters email, clicks next, enters password
     * 5. One-time code after email - navigates to password login via "Try another way"
     *
     * @param email The PayPal account email
     * @param password The PayPal account password
     * @return LoginState indicating the result of the login process
     */
    fun checkout(email: String, password: String, maxRetries: Int = 3): Boolean {
        repeat(maxRetries) {
            Log.d(TAG, "🔐 Starting PayPal checkout flow on web")

            val success = when {
                completeReviewOrder() -> {
                    Log.d(TAG, "✅ User already logged in, finishing checkout without login flow")
                    true
                }

                login(email, password) && completeReviewOrder() -> {
                    Log.d(TAG, "✅ Login successful and review order completed")
                    true
                }

                chromeRobot.dismissFirstTimeSetupIfPresent() && login(
                    email,
                    password
                ) && completeReviewOrder() -> {
                    Log.d(
                        TAG,
                        "✅ Dismissed Chrome setup, login successful and review order completed"
                    )
                    true
                }

                else -> {
                    Log.w(
                        TAG,
                        "⚠️ Login or checkout flow did not complete successfully on attempt ${it + 1}, retrying..."
                    )
                    logPageHierarchy()
                    false
                }
            }

            if (success) {
                return true
            }
            device.waitForIdle()
        }

        Log.e(TAG, "❌ Checkout failed after $maxRetries attempts")
        return false
    }

    private fun login(email: String, password: String): Boolean {
        return enterEmail(email) && enterPassword(password) && clickLoginButton()
    }

    private fun enterEmail(email: String): Boolean {
        findElement(EMAIL_FIELD_SELECTORS)?.clearAndEnterText(email) ?: run {
            Log.w(TAG, "⚠️ Could not find email field to enter email")
            return false
        }
        Log.d(TAG, "✅ Entered email")
        device.waitForIdle()
        return true
    }

    private fun enterPassword(password: String): Boolean {
        when {
            findElement(PASSWORD_FIELD_SELECTORS)?.clearAndEnterText(password) == true -> {
                Log.d(TAG, "✅ Entered password on single-page login")
            }

            clickNextButton() && findElement(PASSWORD_FIELD_SELECTORS)?.clearAndEnterText(password) == true -> {
                Log.d(TAG, "✅ Entered password on two-step login")
            }

            clickTryAnotherWay() && clickUsePasswordInstead() && findElement(
                PASSWORD_FIELD_SELECTORS
            )?.clearAndEnterText(password) == true -> {
                Log.d(
                    TAG,
                    "✅ Entered password after navigating from one-time code via 'Try another way'"
                )
            }

            else -> {
                Log.w(TAG, "⚠️ Could not find password field to enter password")
                return false
            }
        }
        device.waitForIdle()
        return true
    }

    fun completeReviewOrder(): Boolean {
        findElement(REVIEW_ORDER_SELECTORS)?.click() ?: run {
            Log.w(TAG, "⚠️ Could not find review order button - may not be logged in yet")
            return false
        }
        Log.d(TAG, "✅ Clicked review order button")
        device.waitForIdle()
        return true
    }

    private fun clickNextButton(): Boolean {
        findElement(NEXT_BUTTON_SELECTORS)?.click() ?: run {
            Log.w(TAG, "⚠️ Could not find Next/Continue button")
            return false
        }
        Log.d(TAG, "✅ Clicked Next button")
        device.waitForIdle()
        return true
    }

    private fun clickLoginButton(): Boolean {
        findElement(LOGIN_BUTTON_SELECTORS)?.click() ?: run {
            Log.w(TAG, "⚠️ Could not find Login/Sign In button")
            return false
        }
        Log.d(TAG, "✅ Clicked Login button")
        device.waitForIdle(TIMEOUT_LONG_MS)
        return true
    }

    private fun clickTryAnotherWay(): Boolean {
        findElement(TRY_ANOTHER_WAY_SELECTORS)?.click() ?: run {
            Log.w(
                TAG,
                "⚠️ Could not find 'Try another way' button to navigate from one-time code to password login"
            )
            return false
        }
        Log.d(TAG, "✅ Clicked 'Try another way' button to navigate to password login")
        device.waitForIdle()
        return true
    }

    private fun clickUsePasswordInstead(): Boolean {
        findElement(USE_PASSWORD_SELECTORS)?.click() ?: run {
            Log.w(
                TAG,
                "⚠️ Could not find 'Use password instead' button to switch from one-time code to password entry"
            )
            return false
        }
        Log.d(TAG, "✅ Clicked 'Use password instead' button to switch to password entry")
        device.waitForIdle()
        return true
    }

}
