package com.paypal.android.paypalwebpayments

import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.BrowserSwitchAuthChallenge

data class PayPalAuthChallenge(
    override val options: BrowserSwitchOptions,
    internal val analytics: PayPalAnalyticsContext
) : BrowserSwitchAuthChallenge
