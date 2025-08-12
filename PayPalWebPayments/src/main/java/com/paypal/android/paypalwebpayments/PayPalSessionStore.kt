package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.SessionStore

internal class PayPalSessionStore() : SessionStore() {
    var authState: String? by properties

    // for analytics tracking
    var checkoutOrderId: String? by properties
    var vaultSetupTokenId: String? by properties
}
