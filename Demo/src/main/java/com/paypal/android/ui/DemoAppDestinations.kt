package com.paypal.android.ui

object DemoAppDestinations {
    const val CARD_APPROVE_ORDER = "card_approve_order"
    const val FEATURES_ROUTE = "features"
    const val CARD_VAULT = "card_vault"
    const val PAYPAL_CHECKOUT = "paypal_checkout"
    const val PAYPAL_VAULT = "paypal_vault"
    const val PAYPAL_BUTTONS = "paypal_buttons"
    const val PAYPAL_STATIC_BUTTONS = "paypal_static_buttons"
    const val SELECT_TEST_CARD = "select_test_card"
    const val SETTINGS = "settings"

    fun titleForDestination(destination: String?): String = when (destination) {
        CARD_APPROVE_ORDER -> "Card Approve Order"
        FEATURES_ROUTE -> "Features"
        CARD_VAULT -> "Vault Card"
        PAYPAL_CHECKOUT -> "PayPal"
        PAYPAL_BUTTONS -> "PayPal Buttons"
        PAYPAL_STATIC_BUTTONS -> "PayPal Static Buttons"
        SELECT_TEST_CARD -> "Select a Test Card"
        PAYPAL_VAULT -> "Paypal Vault"
        SETTINGS -> "Environment Settings"
        else -> "Demo"
    }
}
