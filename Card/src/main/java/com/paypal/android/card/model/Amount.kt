package com.paypal.android.card.model

import com.paypal.android.core.PaymentsJSON

data class Amount(
    val currencyCode: String?,
    val value: String?
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("currency_code"),
        json.optString("value")
    )
}
