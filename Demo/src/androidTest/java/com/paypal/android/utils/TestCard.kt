package com.paypal.android.utils

/**
 * Enum for test card display names used in PayPal Android SDK tests.
 * These names correspond to the TestCard instances defined in TestCard.kt
 */
enum class TestCard(val displayName: String) {
    VISA_VAULT_WITH_PURCHASE_NO_3DS("🇺🇸 Vault with Purchase (no 3DS)"),
    VISA_NO_3DS("Visa"),
    VISA_NO_3DS_2("New Visa"),
    VISA_3DS_SUCCESSFUL_AUTH("3DS Successful Auth"),
    VISA_3DS_FAILED_SIGNATURE("3DS Failed Signature"),
    VISA_3DS_FAILED_AUTHENTICATION("3DS Failed Authentication"),
    VISA_3DS_PASSIVE_AUTHENTICATION("3DS Passive Authentication"),
    VISA_3DS_TRANSACTION_TIMEOUT("3DS Transaction Timeout"),
    VISA_3DS_NOT_ENROLLED("3DS Not Enrolled"),
    VISA_3DS_AUTH_SYSTEM_UNAVAILABLE("3DS Auth System Unavailable"),
    VISA_3DS_AUTH_ERROR("3DS Auth Error"),
    VISA_3DS_AUTH_UNAVAILABLE("3DS Auth Unavailable"),
    VISA_3DS_MERCHANT_BYPASSED_AUTH("3DS Merchant Bypassed Auth"),
    VISA_3DS_MERCHANT_INACTIVE("3DS Merchant Inactive"),
    VISA_3DS_CMPI_LOOKUP_ERROR("3DS cmpi_lookup Error"),
}
