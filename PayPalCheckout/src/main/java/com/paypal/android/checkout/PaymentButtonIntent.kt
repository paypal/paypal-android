package com.paypal.android.checkout


/**
 * This intent will be used for determining payment button eligibility.
 */
enum class PaymentButtonIntent {

    /**
     * The merchant intends to capture payment immediately after the customer makes a payment.
     */
    CAPTURE,

    /**
     * The merchant intends to authorize a payment and place funds on hold after the customer makes
     * a payment. Authorized payments are guaranteed for up to three days but are available to
     * capture for up to 29 days. After the three-day honor period, the original authorized payment
     * expires and you must re-authorize the payment. You must make a separate request to capture
     * payments on demand. This intent is not supported when you have more than one `purchase_unit`
     * within your order.
     */
    AUTHORIZE,

    /**
     * The merchant intents to set up a subscription.
     */
    SUBSCRIPTION
}

internal val PaymentButtonIntent.asNativeCheckout: com.paypal.checkout.config.PaymentButtonIntent
    get() = enumValueOf(this.name)