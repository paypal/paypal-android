package com.paypal.android

import javax.inject.Inject

class PayPalConfigConstants @Inject constructor() {
    val returnUrl = "${BuildConfig.APPLICATION_ID}://paypalpay"
}
