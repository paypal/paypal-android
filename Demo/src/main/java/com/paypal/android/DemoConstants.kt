package com.paypal.android

object DemoConstants {
    // Made these temporary changes because getting Not verified error when using applink with auth tab
    const val APP_URL = "com.paypal.android.demo://"
    const val APP_FALLBACK_URL_SCHEME = "com.paypal.android.demo"
    const val SUCCESS_URL = "com.paypal.android.demo://success"
    const val CANCEL_URL = "com.paypal.android.demo://cancel"
    const val VAULT_SUCCESS_URL = "$APP_URL/vault/success"
    const val VAULT_CANCEL_URL = "$APP_URL/vault/cancel"
}
