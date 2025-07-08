package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class UpdateClientConfigParams(
    val orderId: String,
    val fundingSource: String
)
