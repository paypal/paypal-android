package com.paypal.android.checkout


/**
 * Configures a Continue or Pay Now checkout flow.
 */
enum class UserAction {
    /**
     * After you redirect the buyer to the PayPal payment page, a Continue button appears.
     *
     * Use this option when the final amount is not known when the checkout flow is initiated and you
     * want to redirect the buyer to the merchant page without processing the payment.
     *
     * This will hide the total price on the PayPal pay sheet.
     */
    CONTINUE,

    /**
     * After you redirect the buyer to the PayPal payment page, a Pay Now button appears.
     *
     * Use this option when the final amount is known when the checkout is initiated and you want to
     * process the payment immediately when the buyer clicks Pay Now.
     *
     * This will display the total price on the PayPal pay sheet.
     */
    PAY_NOW
}

internal val UserAction.asNativeCheckout: com.paypal.checkout.createorder.UserAction
    get() = enumValueOf(this.name)

internal val com.paypal.checkout.createorder.UserAction.asPaypalCheckout: UserAction
    get() = enumValueOf(this.name)
