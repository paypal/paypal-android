package com.paypal.android.ui

object DemoAppDestinations {
    const val CARD_APPROVE_ORDER = "card_approve_order"
    const val FEATURES_ROUTE = "features"
    const val CARD_VAULT = "card_vault"
    const val PAYPAL_WEB = "paypal_web"
    const val PAYPAL_BUTTONS = "paypal_buttons"
    const val PAYPAL_NATIVE = "paypal_native"
    const val SELECT_TEST_CARD = "select_test_card"

    fun titleForDestination(destination: String?): String = when (destination) {
        CARD_APPROVE_ORDER -> "Card Approve Order"
        FEATURES_ROUTE -> "Features"
        CARD_VAULT -> "Vault Card"
        PAYPAL_WEB -> "PayPal Web"
        PAYPAL_BUTTONS -> "PayPal Buttons"
        PAYPAL_NATIVE -> "PayPal Native"
        SELECT_TEST_CARD -> "Select a Test Card"
        else -> "Demo"
    }
}
