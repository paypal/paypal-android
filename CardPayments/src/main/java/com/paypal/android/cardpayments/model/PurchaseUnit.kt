package com.paypal.android.cardpayments.model

import com.paypal.android.corepayments.PaymentsJSON

/**
 * A purchase unit for an order. If an order takes multiple purchase units,
 * each one must contain a reference id.
 */
internal data class PurchaseUnit(
    val referenceId: String?,
    val amount: Amount? = null,
    val payee: Payee? = null
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("reference_id"),
        json.optMapObject("amount") { Amount(it) },
        json.optMapObject("payee") { Payee(json) }
    )
}
