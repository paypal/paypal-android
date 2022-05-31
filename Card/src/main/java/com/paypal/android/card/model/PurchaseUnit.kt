package com.paypal.android.card.model

import com.paypal.android.core.optNullableString
import org.json.JSONArray
import org.json.JSONObject

/**
 * A purchase unit for an order. If an order takes multiple purchase units,
 * each one must contain a reference id.
 */
data class PurchaseUnit(
    val referenceId: String?,
    val amount: Amount,
    val payee: Payee? = null
) {
    internal constructor(json: JSONObject) : this(
        json.optNullableString("reference_id"),
        Amount(json),
        Payee(json)
    )
}
