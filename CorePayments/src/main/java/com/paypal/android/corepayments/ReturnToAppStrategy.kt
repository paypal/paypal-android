package com.paypal.android.corepayments

/**
 * Strategy for returning to the app after browser-based checkout flows.
 *
 * Use [AppLink] for Android App Links (https:// URLs) or [CustomUrlScheme] for custom URL schemes.
 */
sealed class ReturnToAppStrategy {
    /**
     * Return to app using Android App Links.
     *
     * @property appLinkUrl The app link URL to use for browser switch.
     *   Example: "https://example.com/path"
     */
    data class AppLink(val appLinkUrl: String) : ReturnToAppStrategy()

    /**
     * Return to app using a custom URL scheme.
     *
     * @property urlScheme The custom URL scheme to use for browser switch.
     *   Example: "myapp"
     */
    data class CustomUrlScheme(val urlScheme: String) : ReturnToAppStrategy()
}
