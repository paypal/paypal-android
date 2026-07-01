package com.paypal.android.paypalwebpayments

/**
 * Controls the call-to-action label displayed on the PayPal checkout page.
 *
 * Passed to [PayPalWebCheckoutClient.createPayPalSession].
 */
enum class PayPalUserAction {
    /** Displays a "Continue" button. Default for most checkout flows. */
    CONTINUE,

    /** Displays a "Pay Now" button. Use when the order amount is final at checkout. */
    PAY_NOW,

    /** Displays a "Set Up" button. Use for vault-without-purchase flows. */
    SETUP_NOW,
}
