package com.paypal.android.core

data class OrderErrorDetail(
    val issue: String,
    val description: String
) {
    override fun toString(): String {
        return "Issue: $issue." +
                "\nError description: ${description}."
    }
}
