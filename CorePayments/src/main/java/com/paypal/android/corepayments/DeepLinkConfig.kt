package com.paypal.android.corepayments

sealed class DeepLinkConfig {
    data class CustomUrlScheme(val scheme: String) : DeepLinkConfig()
    data class AppLink(val url: String, val fallback: CustomUrlScheme? = null) : DeepLinkConfig()
}