package com.paypal.android.card.model

import com.paypal.android.core.PaymentsJSON

data class Payee(val emailAddress: String) {
    internal constructor(json: PaymentsJSON) : this(json.getString("email_address"))
}
