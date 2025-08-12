package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.SessionStore

internal class PayPalSessionStore() : SessionStore() {
    internal var authState: String? by properties

    // for analytics tracking
    internal var checkoutOrderId: String? by properties
    internal var vaultSetupTokenId: String? by properties
}
