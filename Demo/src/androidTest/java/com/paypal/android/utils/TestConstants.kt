package com.paypal.android.utils

/**
 * Centralized constants for all test timing values
 */
object TestConstants {

    /**
     * Standard short timeout for quick operations (3 seconds)
     * Used for: Chrome dialog detection, browser cache clearing, button interactions
     */
    const val TIMEOUT_SHORT_MS = 3_000L

    /**
     * Standard long timeout for extended operations (10 seconds)
     * Used for: Compose UI element waits, web page loads, app state transitions
     */
    const val TIMEOUT_LONG_MS = 20_000L
}
