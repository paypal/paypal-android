package com.paypal.android.api.model

import kotlinx.serialization.Serializable

/**
 * The call-to-action label on the PayPal checkout page, sent as
 * `payment_source.paypal.experience_context.user_action` on order creation.
 */
@Serializable
enum class UserActionSelected {
    /** Displays a "Continue" button. */
    CONTINUE,

    /** Displays a "Pay Now" button. Use when the order amount is final at checkout. */
    PAY_NOW,

    /** Displays a "Set Up" button. Use for vault-without-purchase flows. */
    SETUP_NOW
}
