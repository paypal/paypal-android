package com.paypal.android.core

data class Address(
    val countryCode: String,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val postalCode: String? = null
)
