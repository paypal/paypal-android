package com.paypal.android.core

enum class Environment(val url: String) {
    LIVE("https://api.paypal.com"),
    SANDBOX("https://api.sandbox.paypal.com")
}
