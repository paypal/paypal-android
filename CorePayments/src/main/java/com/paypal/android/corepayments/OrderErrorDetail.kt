package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OrderErrorDetail(
    val issue: String,
    val description: String
) {
    override fun toString(): String {
        return "Issue: $issue.\n" +
                "Error description: $description"
    }
}
