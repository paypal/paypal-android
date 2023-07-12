package com.paypal.android.cardpayments

data class VaultRequest(val card: Card, val customerId: String? = null)
