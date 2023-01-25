package com.paypal.android.card.model

import com.paypal.android.corepayments.PaymentsJSON

data class Payee(val emailAddress: String) {
    internal constructor(json: PaymentsJSON) : this(json.optString("email_address") ?: "")
}
