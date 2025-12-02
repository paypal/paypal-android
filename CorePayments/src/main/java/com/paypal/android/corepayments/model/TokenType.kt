package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class TokenType {
    ORDER_ID,
    VAULT_ID,
    BILLING_TOKEN
}
