package com.paypal.android.corepayments.models

import com.paypal.android.corepayments.PaymentsJSON

data class Amount(
    val currencyCode: String?,
    val value: String?
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("currency_code"),
        json.optString("value")
    )
}
