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
 * Enters email/username in PayPal login page
 * @param email The email address to enter
 * @param timeout Timeout in milliseconds (default 30 seconds)
 * @return true if email was entered successfully, false otherwise
 */
fun enterPayPalEmail(email: String, timeout: Long = 30_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    // Wait for PayPal login page to load
    // Look for email/username input field
    val emailField = device.wait(
        Until.findObject(By.res("email")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.res("login_emailField")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.clazz("android.widget.EditText")),
        timeout
    )

    if (emailField != null) {
        Log.d(tag, "✅ Found email field, entering: $email")
        emailField.text = email
        Thread.sleep(1000) // Wait for input to register

        // Look for Next or Continue button
        val nextButton = device.findObject(By.text("Next"))
            ?: device.findObject(By.textContains("Continue"))
            ?: device.findObject(By.res("btnNext"))

        if (nextButton != null) {
            nextButton.click()
            Log.d(tag, "✅ Clicked Next button")
            Thread.sleep(2000) // Wait for navigation to password page
            return true
        } else {
            Log.w(tag, "⚠️ Could not find Next/Continue button")
            return false
        }
    } else {
        Log.w(tag, "⚠️ Could not find email field")
        return false
    }
}

/**
 * Enters password in PayPal login page
 * @param password The password to enter
 * @param timeout Timeout in milliseconds (default 30 seconds)
 * @return true if password was entered and login submitted successfully, false otherwise
 */
fun enterPayPalPassword(password: String, timeout: Long = 30_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    // Wait for password field
    val passwordField = device.wait(
        Until.findObject(By.res("password")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.res("login_passwordField")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.clazz("android.widget.EditText")),
        timeout
    )

    if (passwordField != null) {
        Log.d(tag, "✅ Found password field, entering password")
        passwordField.text = password
        Thread.sleep(1000) // Wait for input to register

        // Look for Login button
        val loginButton = device.findObject(By.text("Log In"))
            ?: device.findObject(By.textContains("Sign In"))
            ?: device.findObject(By.res("btnLogin"))

        if (loginButton != null) {
            loginButton.click()
            Log.d(tag, "✅ Clicked Login button")
            Thread.sleep(3000) // Wait for login to process
            return true
        } else {
            Log.w(tag, "⚠️ Could not find Login/Sign In button")
            return false
        }
    } else {
        Log.w(tag, "⚠️ Could not find password field")
        return false
    }
}

/**
 * Clicks "Try another way" button on PayPal 2FA/security verification page
 * @param timeout Timeout in milliseconds (default 10 seconds)
 * @return true if button was found and clicked, false otherwise
 */
fun clickTryAnotherWay(timeout: Long = 10_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    // Wait for "Try another way" button to appear
    val tryAnotherWayButton = device.wait(
        Until.findObject(By.text("Try another way")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("another way")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("different")),
        timeout
    )

    if (tryAnotherWayButton != null) {
        Log.d(tag, "✅ Found 'Try another way' button, clicking it")
        tryAnotherWayButton.click()
        Thread.sleep(2000) // Wait for navigation
        return true
    } else {
        Log.d(tag, "ℹ️ 'Try another way' button not found - may not be on 2FA page")
        return false
    }
}

/**
 * Clicks "Use password instead" button on PayPal login alternative methods modal
 * @param timeout Timeout in milliseconds (default 10 seconds)
 * @return true if button was found and clicked, false otherwise
 */
fun clickUsePasswordInstead(timeout: Long = 10_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    // Wait for "Use password instead" button to appear
    val usePasswordButton = device.wait(
        Until.findObject(By.text("Use password instead")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("password instead")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("Use password")),
        timeout
    )

    if (usePasswordButton != null) {
        Log.d(tag, "✅ Found 'Use password instead' button, clicking it")
        usePasswordButton.click()
        Thread.sleep(2000) // Wait for password page to load
        return true
    } else {
        Log.w(tag, "⚠️ 'Use password instead' button not found")
        return false
    }
}

/**
 * Checks if we're on the PayPal review order page
 * @param timeout Timeout in milliseconds (default 3 seconds)
 * @return true if on review order page, false otherwise
 */
fun isOnReviewOrderPage(timeout: Long = 3_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    val reviewOrderIndicators = device.wait(
        Until.findObject(By.textContains("Review")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("Continue")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("Pay Now")),
        timeout
    )

    val isOnPage = reviewOrderIndicators != null
    if (isOnPage) {
        Log.d(tag, "✅ Already on review order page - user is logged in")
    }
    return isOnPage
}

/**
 * Checks if we're on the PayPal email/login page
 * @param timeout Timeout in milliseconds (default 3 seconds)
 * @return true if on login page, false otherwise
 */
fun isOnLoginPage(timeout: Long = 3_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    val emailField = device.wait(
        Until.findObject(By.res("email")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.res("login_emailField")),
        timeout
    )

    val isOnPage = emailField != null
    if (isOnPage) {
        Log.d(tag, "✅ On login page - email field found")
    }
    return isOnPage
}

/**
 * Checks if we're on the one-time code page (requires "Try another way")
 * @param timeout Timeout in milliseconds (default 3 seconds)
 * @return true if on one-time code page, false otherwise
 */
fun isOnOneTimeCodePage(timeout: Long = 3_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    val oneTimeCodeIndicators = device.wait(
        Until.findObject(By.textContains("one-time code")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.textContains("Get a Code")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.text("Try another way")),
        timeout
    )

    val isOnPage = oneTimeCodeIndicators != null
    if (isOnPage) {
        Log.d(tag, "✅ On one-time code page")
    }
    return isOnPage
}

/**
 * Checks if we're on the password page
 * @param timeout Timeout in milliseconds (default 3 seconds)
 * @return true if on password page, false otherwise
 */
fun isOnPasswordPage(timeout: Long = 3_000L): Boolean {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val tag = "PayPalLoginUtils"

    val passwordField = device.wait(
        Until.findObject(By.res("password")),
        timeout
    ) ?: device.wait(
        Until.findObject(By.res("login_passwordField")),
        timeout
    )

    val isOnPage = passwordField != null
    if (isOnPage) {
        Log.d(tag, "✅ On password page")
    }
    return isOnPage
}

/**
 * Complete PayPal login flow by entering email and password
 * Intelligently handles different scenarios:
 * 1. Already logged in - skips login
 * 2. Not logged in - enters credentials
 * 3. One-time code page - clicks "Try another way" -> "Use password instead"
 * 4. Direct password page - enters password directly
 * 5. Single page login - username and password on same page
 *
 * @param email The email address to use for login
 * @param password The password to use for login
 * @param timeout Timeout in milliseconds (default 30 seconds)
 * @return true if login was completed successfully, false otherwise
 */
fun loginToPayPal(email: String, password: String, timeout: Long = 30_000L): Boolean {
    val tag = "PayPalLoginUtils"
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    Log.d(tag, "🔐 Starting PayPal login flow")

    // Wait a moment for page to load
    Thread.sleep(2000)

    // Scenario 1: Check if already on review order page (already logged in)
    if (isOnReviewOrderPage(timeout = 5000L)) {
        Log.d(tag, "✅ User already logged in, skipping login flow")
        return true
    }

    // Scenario 2: Check if on login page (not logged in)
    if (isOnLoginPage(timeout = 5000L)) {
        // Check if password field is ALSO present (single-page login form)
        val passwordFieldPresent = device.findObject(By.res("password")) != null ||
                device.findObject(By.res("login_passwordField")) != null

        if (passwordFieldPresent) {
            // Scenario 2a: Single-page login form (username and password on same page)
            Log.d(
                tag,
                "📧🔑 Single-page login form detected - both email and password fields present"
            )

            // Enter email
            val emailField = device.findObject(By.res("email"))
                ?: device.findObject(By.res("login_emailField"))
                ?: device.findObject(By.clazz("android.widget.EditText"))

            if (emailField != null) {
                Log.d(tag, "✅ Entering email: $email")
                emailField.text = email
                Thread.sleep(1000)
            } else {
                Log.e(tag, "❌ Could not find email field")
                return false
            }

            // Enter password directly
            val passwordEntered = enterPayPalPassword(password, timeout)
            if (!passwordEntered) {
                Log.e(tag, "❌ Failed to enter password")
                return false
            }
        } else {
            // Scenario 2b: Multi-page login form (email on first page)
            Log.d(tag, "📧 Multi-page login form - entering email first")
            val emailEntered = enterPayPalEmail(email, timeout)
            if (!emailEntered) {
                Log.e(tag, "❌ Failed to enter email")
                return false
            }

            // Wait for next page after email
            Thread.sleep(2000)

            // Scenario 2b-i: Check if on one-time code page
            if (isOnOneTimeCodePage(timeout = 5000L)) {
                Log.d(tag, "📱 On one-time code page, clicking 'Try another way'")
                val tryAnotherWayClicked = clickTryAnotherWay(timeout = 5000L)
                if (tryAnotherWayClicked) {
                    Log.d(tag, "✅ Clicked 'Try another way'")

                    // Click "Use password instead" from the modal
                    val usePasswordClicked = clickUsePasswordInstead(timeout = 5000L)
                    if (usePasswordClicked) {
                        Log.d(tag, "✅ Clicked 'Use password instead' button")
                    } else {
                        Log.w(tag, "⚠️ Could not find 'Use password instead' button")
                    }
                }
            } else if (isOnPasswordPage(timeout = 5000L)) {
                // Scenario 2b-ii: Directly on password page
                Log.d(tag, "🔑 Directly on password page")
            } else {
                Log.w(tag, "⚠️ Unexpected page after entering email")
            }

            // Enter password
            val passwordEntered = enterPayPalPassword(password, timeout)
            if (!passwordEntered) {
                Log.e(tag, "❌ Failed to enter password")
                return false
            }
        }
    } else {
        Log.w(tag, "⚠️ Not on login page or review order page - unexpected state")
        return false
    }

    Log.d(tag, "✅ PayPal login completed successfully")
    return true
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
