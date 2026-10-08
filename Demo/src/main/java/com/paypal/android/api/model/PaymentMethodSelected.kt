package com.paypal.android.api.model

import kotlinx.serialization.Serializable

/**
 * The payment method the shopper selected on the PayPal Checkout screen, sent as
 * `payment_source.paypal.experience_context.payment_method_selected` on order creation.
 */
@Serializable
enum class PaymentMethodSelected {
    /** Standard, one-time PayPal Checkout flow. */
    PAYPAL,

    /** Displays Pay Later offers to eligible customers. */
    PAYPAL_PAY_LATER,

    /** Displays PayPal Credit financing to eligible customers. */
    PAYPAL_CREDIT
}
