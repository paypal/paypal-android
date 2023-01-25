package com.paypal.android.corepayments

data class OrderErrorDetail(
    val issue: String,
    val description: String
) {
    override fun toString(): String {
        return "Issue: $issue.\n" +
                "Error description: $description"
    }
}
