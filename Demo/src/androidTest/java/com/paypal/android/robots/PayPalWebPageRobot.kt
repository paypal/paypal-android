package com.paypal.android.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

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

    companion object {
        private const val TAG = "PayPalWebPageRobot"

        // Timeout constants
        private const val WEB_TIMEOUT_MS = 30_000L
        private const val LOGIN_FLOW_TIMEOUT_MS = 5_000L
        private const val NAVIGATION_TIMEOUT_MS = 10_000L

        // Delay constants
        private const val PAGE_LOAD_DELAY_MS = 2_000L
        private const val POST_EMAIL_DELAY_MS = 3_000L
        private const val MODAL_APPEAR_DELAY_MS = 2_000L
        private const val INPUT_CLEAR_DELAY_MS = 500L
        private const val INPUT_REGISTER_DELAY_MS = 1_000L
        private const val BUTTON_CLICK_DELAY_MS = 2_000L
        private const val LOGIN_PROCESS_DELAY_MS = 3_000L

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
            By.textContains("Review"),
            By.textContains("Continue"),
            By.textContains("Pay Now")
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

        private val ONE_TIME_CODE_SELECTORS = listOf(
            By.textContains("one-time code"),
            By.textContains("Get a Code"),
            By.text("Try another way")
        )
    }

    // ========== Public API ==========

    /**
     * Performs the complete PayPal login flow.
     * Intelligently handles different scenarios:
     * 1. Already logged in - skips login
     * 2. Direct one-time code page - navigates to password, enters password
     * 3. Single-page login - enters email and password directly
     * 4. Two-step login - enters email, clicks next, enters password
     * 5. One-time code after email - navigates to password login via "Try another way"
     *
     * @param email The PayPal account email
     * @param password The PayPal account password
     * @return true if login was completed successfully, false otherwise
     */
    fun performLogin(email: String, password: String): Boolean {
        Log.d(TAG, "🔐 Starting PayPal login flow")

        waitForPageLoad()

        // Scenario 1: Already logged in
        if (isOnReviewOrderPage()) {
            Log.d(TAG, "✅ User already logged in, skipping login flow")
            return true
        }

        // Scenario 2: On one-time code page (email already entered in previous session)
        if (isOnOneTimeCodePage()) {
            Log.d(TAG, "📱 Landed directly on one-time code page")
            if (!handleOneTimeCodeNavigation()) {
                return false
            }
            // After navigating from one-time code, enter password
            if (!enterPassword(password)) {
                Log.e(TAG, "❌ Failed to enter password")
                return false
            }
            Log.d(TAG, "✅ PayPal login completed successfully")
            return true
        }

        // Scenario 3: On login page - handle normal login
        if (!isOnLoginPage()) {
            Log.w(TAG, "⚠️ Not on login page or review order page - unexpected state")
            return false
        }

        return handleLoginFlow(email, password)
    }

    /**
     * Completes the PayPal review order page by clicking the checkout button
     */
    fun completeReviewOrder() {
        val reviewOrderButton = findElement(REVIEW_ORDER_SELECTORS, WEB_TIMEOUT_MS)

        if (reviewOrderButton != null) {
            Log.d(TAG, "✅ Found PayPal checkout button in browser")
            reviewOrderButton.click()
            Thread.sleep(BUTTON_CLICK_DELAY_MS)
        } else {
            Log.w(
                TAG,
                "⚠️ Could not find PayPal checkout button - may be in app switch or sandbox mode"
            )
        }
    }

    // ========== Login Flow Orchestration ==========

    private fun handleLoginFlow(email: String, password: String): Boolean {
        Log.d(TAG, "📧 On login page, entering email")

        if (!enterEmail(email)) {
            Log.e(TAG, "❌ Failed to enter email")
            return false
        }

        if (!handlePasswordNavigation()) {
            return false
        }

        if (!enterPassword(password)) {
            Log.e(TAG, "❌ Failed to enter password")
            return false
        }

        Log.d(TAG, "✅ PayPal login completed successfully")
        return true
    }

    private fun handlePasswordNavigation(): Boolean {
        // Check if password field is already visible (single-page login)
        if (isOnPasswordPage()) {
            Log.d(TAG, "🔑 Single-page login - password field already visible, skipping navigation")
            return true
        }

        // Wait for next page after email (two-step login)
        Thread.sleep(POST_EMAIL_DELAY_MS)

        // Check if on one-time code page
        if (isOnOneTimeCodePage()) {
            return handleOneTimeCodeNavigation()
        }

        // Check if on password page after clicking Next
        if (isOnPasswordPage()) {
            Log.d(TAG, "🔑 Directly on password page")
            return true
        }

        Log.w(TAG, "⚠️ Unexpected page after entering email")
        return true // Continue anyway, enterPassword will fail if no password field
    }

    private fun handleOneTimeCodeNavigation(): Boolean {
        Log.d(TAG, "📱 On one-time code page, clicking 'Try another way'")

        if (!clickTryAnotherWay()) {
            Log.w(TAG, "⚠️ Failed to click 'Try another way' button")
            return true // Continue anyway, may still work
        }

        Log.d(TAG, "✅ Clicked 'Try another way'")
        Thread.sleep(MODAL_APPEAR_DELAY_MS)

        if (clickUsePasswordInstead()) {
            Log.d(TAG, "✅ Clicked 'Use password instead' button")
        } else {
            Log.w(TAG, "⚠️ Could not find 'Use password instead' button")
        }

        return true
    }

    // ========== Page Detection Methods ==========

    private fun isOnReviewOrderPage(): Boolean {
        val element = findElement(REVIEW_ORDER_SELECTORS, LOGIN_FLOW_TIMEOUT_MS)
        val isOnPage = element != null
        if (isOnPage) {
            Log.d(TAG, "✅ Already on review order page - user is logged in")
        }
        return isOnPage
    }

    private fun isOnLoginPage(): Boolean {
        val emailField = findElement(EMAIL_FIELD_SELECTORS, LOGIN_FLOW_TIMEOUT_MS)
        val isOnPage = emailField != null
        if (isOnPage) {
            Log.d(TAG, "✅ On login page - email field found")
        }
        return isOnPage
    }

    private fun isOnOneTimeCodePage(): Boolean {
        Log.d(TAG, "🔍 Checking if on one-time code page...")
        val element = findElement(ONE_TIME_CODE_SELECTORS, LOGIN_FLOW_TIMEOUT_MS)
        val isOnPage = element != null
        if (isOnPage) {
            Log.d(TAG, "✅ Detected one-time code page")
        } else {
            Log.d(TAG, "ℹ️ Not on one-time code page")
        }
        return isOnPage
    }

    private fun isOnPasswordPage(): Boolean {
        val passwordField = findElement(PASSWORD_FIELD_SELECTORS, LOGIN_FLOW_TIMEOUT_MS)
        val isOnPage = passwordField != null
        if (isOnPage) {
            Log.d(TAG, "✅ On password page")
        }
        return isOnPage
    }

    // ========== Form Input Methods ==========

    private fun enterEmail(email: String): Boolean {
        val emailField = findElement(
            EMAIL_FIELD_SELECTORS + By.clazz("android.widget.EditText"),
            WEB_TIMEOUT_MS
        )

        if (emailField == null) {
            Log.w(TAG, "⚠️ Could not find email field")
            return false
        }

        // Always clear and re-enter email for test consistency
        clearAndEnterText(emailField, email, "Email")
        Log.d(TAG, "✅ Entered email: $email")

        // Check if password field is already visible (single-page login)
        if (isOnPasswordPage()) {
            Log.d(TAG, "✅ Password field already visible - single-page login detected")
            return true
        }

        // Look for Next or Continue button (two-step login)
        return clickNextButton()
    }

    private fun enterPassword(password: String): Boolean {
        val passwordField = findElement(
            PASSWORD_FIELD_SELECTORS + By.clazz("android.widget.EditText"),
            WEB_TIMEOUT_MS
        )

        if (passwordField == null) {
            Log.w(TAG, "⚠️ Could not find password field")
            return false
        }

        // Always clear and re-enter password for test consistency
        clearAndEnterText(passwordField, password, "Password")
        Log.d(TAG, "✅ Entered password")

        // Click login button
        return clickLoginButton()
    }

    // ========== Navigation Methods ==========

    private fun clickNextButton(): Boolean {
        val nextButton = findElementNoWait(NEXT_BUTTON_SELECTORS)

        if (nextButton == null) {
            Log.w(TAG, "⚠️ Could not find Next/Continue button")
            return false
        }

        nextButton.click()
        Log.d(TAG, "✅ Clicked Next button")
        Thread.sleep(BUTTON_CLICK_DELAY_MS)
        return true
    }

    private fun clickLoginButton(): Boolean {
        val loginButton = findElementNoWait(LOGIN_BUTTON_SELECTORS)

        if (loginButton == null) {
            Log.w(TAG, "⚠️ Could not find Login/Sign In button")
            return false
        }

        loginButton.click()
        Log.d(TAG, "✅ Clicked Login button")
        Thread.sleep(LOGIN_PROCESS_DELAY_MS)
        return true
    }

    private fun clickTryAnotherWay(): Boolean {
        Log.d(TAG, "🔍 Looking for 'Try another way' button...")
        val button = findElement(TRY_ANOTHER_WAY_SELECTORS, NAVIGATION_TIMEOUT_MS)

        if (button == null) {
            Log.w(
                TAG,
                "❌ 'Try another way' button not found after waiting ${NAVIGATION_TIMEOUT_MS}ms"
            )
            return false
        }

        Log.d(TAG, "✅ Found 'Try another way' button, clicking it")
        button.click()
        Thread.sleep(BUTTON_CLICK_DELAY_MS)
        return true
    }

    private fun clickUsePasswordInstead(): Boolean {
        Log.d(TAG, "🔍 Looking for 'Use password instead' button...")
        val button = findElement(USE_PASSWORD_SELECTORS, NAVIGATION_TIMEOUT_MS)

        if (button == null) {
            Log.w(
                TAG,
                "❌ 'Use password instead' button not found after waiting ${NAVIGATION_TIMEOUT_MS}ms"
            )
            return false
        }

        Log.d(TAG, "✅ Found 'Use password instead' button, clicking it")
        button.click()
        Thread.sleep(BUTTON_CLICK_DELAY_MS)
        return true
    }

    // ========== Utility Methods ==========

    /**
     * Finds an element using multiple selectors with a timeout.
     * Tries each selector in order until one is found.
     */
    private fun findElement(selectors: List<BySelector>, timeout: Long): UiObject2? {
        for (selector in selectors) {
            val element = device.wait(Until.findObject(selector), timeout)
            if (element != null) {
                return element
            }
        }
        return null
    }

    /**
     * Finds an element using multiple selectors without waiting.
     * Returns immediately if found or null if not found.
     */
    private fun findElementNoWait(selectors: List<BySelector>): UiObject2? {
        for (selector in selectors) {
            val element = device.findObject(selector)
            if (element != null) {
                return element
            }
        }
        return null
    }

    /**
     * Clears an input field and enters new text with appropriate delays
     */
    private fun clearAndEnterText(field: UiObject2, text: String, fieldName: String) {
        val currentText = field.text ?: ""
        if (currentText.isNotEmpty()) {
            Log.d(TAG, "📧 $fieldName field pre-filled with: $currentText, clearing and re-entering")
        }
        field.clear()
        Thread.sleep(INPUT_CLEAR_DELAY_MS)
        field.text = text
        Thread.sleep(INPUT_REGISTER_DELAY_MS)
    }

    /**
     * Waits for initial page load
     */
    private fun waitForPageLoad() {
        Thread.sleep(PAGE_LOAD_DELAY_MS)
    }
}
