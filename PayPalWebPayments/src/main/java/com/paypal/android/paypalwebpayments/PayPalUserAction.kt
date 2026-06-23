package com.paypal.android.paypalwebpayments

/**
 * Controls the call-to-action text shown on the PayPal checkout page.
 *
 * - [CONTINUE] — Default. Shows "Continue" on the review page.
 * - [PAY_NOW] — Shows "Pay Now" for immediate payment flows.
 * - [SETUP_NOW] — Shows "Set Up" for vault / billing-agreement flows.
 */
enum class PayPalUserAction {
    CONTINUE,
    PAY_NOW,
    SETUP_NOW
}
