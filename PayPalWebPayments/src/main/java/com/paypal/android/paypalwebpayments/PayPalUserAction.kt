package com.paypal.android.paypalwebpayments

/**
 * Controls the call-to-action label shown on the PayPal checkout page.
 */
enum class PayPalUserAction {

    /** Default. Buyer sees a "Continue" button — order capture happens server-side later. */
    CONTINUE,

    /** Buyer sees a "Pay Now" button — order is captured immediately upon approval. */
    PAY_NOW,

    /** Buyer sees a "Set Up" button — used for vault/billing-agreement flows. */
    SETUP_NOW,
}
