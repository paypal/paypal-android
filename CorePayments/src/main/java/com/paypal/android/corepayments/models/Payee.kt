package com.paypal.android.corepayments.models

import com.paypal.android.corepayments.PaymentsJSON

data class Payee(val emailAddress: String) {
    internal constructor(json: PaymentsJSON) : this(json.optString("email_address") ?: "")
}
