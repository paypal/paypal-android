package com.paypal.android.api.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeepLinkStrategy {
    APP_LINK,
    CUSTOM_URL_SCHEME
}
