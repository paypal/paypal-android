package com.paypal.android.corepayments

object PayPalSDK {

    var coreConfig: CoreConfig? = null
        private set

    fun init(coreConfig: CoreConfig) {
        this.coreConfig = coreConfig
    }
}
