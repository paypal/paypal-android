package com.paypal.android.corepayments.browserswitch

import kotlinx.serialization.Serializable

@Serializable
enum class BrowserSwitchRequestCode {
    CARD_APPROVE_ORDER,
    CARD_VAULT,
    PAYPAL_CHECKOUT,
    PAYPAL_VAULT,
}