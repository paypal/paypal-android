package com.paypal.android.paypalwebpayments

import com.braintreepayments.api.BrowserSwitchOptions

data class PayPalWebAuthChallenge(
    internal val browserSwitchOptions: BrowserSwitchOptions
)