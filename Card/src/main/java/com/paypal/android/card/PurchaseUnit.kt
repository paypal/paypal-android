package com.paypal.android.card

/**
 * A purchase unit for an order. If an order takes multiple purchase units,
 * each one must contain a reference id.
 */
data class PurchaseUnit(
    val referenceId: String,
    val amount: Amount
)
