package com.paypal.android.card

data class CardAttributes(
    val customerId: String? = null,
    val customerEmail: String? = null,
    val vaultOnOrderCompletion: Boolean = false,
    val verificationMethod: CardVerificationMethod? = null
)
