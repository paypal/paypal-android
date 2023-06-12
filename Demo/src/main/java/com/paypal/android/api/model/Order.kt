package com.paypal.android.api.model

data class Order(
    val id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    val cardLast4: String? = null,
    val cardBrand: String? = null,
    val vaultId: String? = null,
    val customerId: String? = null
)
