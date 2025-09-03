package com.paypal.android.cardpayments.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Verification methods for 3D Secure authentication
 */
@Serializable
enum class VerificationMethod {
    @SerialName("SCA_ALWAYS")
    SCA_ALWAYS,

    @SerialName("SCA_WHEN_REQUIRED")
    SCA_WHEN_REQUIRED
}

/**
 * Data classes for Kotlin serialization of confirm payment source REST API request
 */
@InternalSerializationApi
@Serializable
internal data class ConfirmPaymentSourceRequest(
    @SerialName("payment_source")
    val paymentSource: PaymentSource,
    @SerialName("application_context")
    val applicationContext: ApplicationContext
)

@InternalSerializationApi
@Serializable
internal data class PaymentSource(
    val card: CardPaymentSource
)

@InternalSerializationApi
@Serializable
internal data class CardPaymentSource(
    val name: String? = null,
    val number: String,
    val expiry: String,
    @SerialName("security_code")
    val securityCode: String?,
    @SerialName("billing_address")
    val billingAddress: BillingAddress? = null,
    val attributes: CardAttributes? = null
)

@InternalSerializationApi
@Serializable
internal data class BillingAddress(
    @SerialName("address_line_1")
    val addressLine1: String? = null,
    @SerialName("address_line_2")
    val addressLine2: String? = null,
    @SerialName("admin_area_2")
    val adminArea2: String? = null,
    @SerialName("admin_area_1")
    val adminArea1: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("country_code")
    val countryCode: String
)

@InternalSerializationApi
@Serializable
internal data class CardAttributes(
    val verification: Verification? = null
)

@InternalSerializationApi
@Serializable
internal data class Verification(
    val method: VerificationMethod
)

@InternalSerializationApi
@Serializable
internal data class ApplicationContext(
    @SerialName("return_url")
    val returnUrl: String,
    @SerialName("cancel_url")
    val cancelUrl: String
)
