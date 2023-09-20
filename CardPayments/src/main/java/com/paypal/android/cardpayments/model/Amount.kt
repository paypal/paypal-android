package com.paypal.android.cardpayments.model

import com.paypal.android.corepayments.PaymentsJSON

internal data class Amount(
    val currencyCode: String?,
    val value: String?
) {
    internal constructor(json: PaymentsJSON) : this(
        json.optString("currency_code"),
        json.optString("value")
    )
}
