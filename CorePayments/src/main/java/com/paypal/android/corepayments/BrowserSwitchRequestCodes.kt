package com.paypal.android.corepayments

// TODO: consider breaking change to migrate away from a centralized set of request codes
// to module-specific request keys to prevent each module from having to depend on :Core
// for its request codes; there will be risk of collision, but proper namespacing can alleviate this
// concern e.g. "PayPal.Checkout", "Venmo.Vault" etc.
object BrowserSwitchRequestCodes {
    const val CARD_APPROVE_ORDER = 1
    const val CARD_VAULT = 2

    const val PAYPAL_CHECKOUT = 3
    const val PAYPAL_VAULT = 4
}
