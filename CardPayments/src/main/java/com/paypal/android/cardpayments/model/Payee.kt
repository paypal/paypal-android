package com.paypal.android.cardpayments.model

import com.paypal.android.corepayments.PaymentsJSON

data class Payee(val emailAddress: String) {
    internal constructor(json: PaymentsJSON) : this(json.optString("email_address") ?: "")
}
