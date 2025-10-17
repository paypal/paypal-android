package com.paypal.android.api.model.serialization

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.PayPalPaymentToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Order Response Models
@Serializable
data class OrderResponse(
    val id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    @SerialName("payment_source")
    val paymentSource: PaymentSourceResponse? = null
)

// Setup Token Response Models
@Serializable
data class SetupTokenResponse(
    val id: String,
    val status: String,
    val customer: CustomerResponse,
    @SerialName("payment_source")
    val paymentSource: PaymentSourceResponse? = null
)

// Common Response Models
@Serializable
data class CustomerResponse(
    val id: String
)

@Serializable
data class PaymentSourceResponse(
    val card: CardResponse? = null
)

@Serializable
data class CardResponse(
    @SerialName("last_digits")
    val lastDigits: String? = null,
    val brand: String? = null,
    val attributes: CardAttributesResponse? = null,
    @SerialName("verification_status")
    val verificationStatus: String? = null
)

@Serializable
data class CardAttributesResponse(
    val vault: VaultResponse? = null
)

@Serializable
data class VaultResponse(
    val id: String? = null,
    val customer: CustomerResponse? = null
)

// Payment Token Response Models
@Serializable
data class PaymentTokenResponse(
    val id: String,
    val customer: CustomerResponse,
    @SerialName("payment_source")
    val paymentSource: PaymentSourceResponse? = null
)

// Extension functions
fun OrderResponse.toOrder(): Order {
    return Order(
        id = id,
        intent = intent,
        status = status,
        lastDigits = paymentSource?.card?.lastDigits,
        cardBrand = paymentSource?.card?.brand,
        vaultId = paymentSource?.card?.attributes?.vault?.id,
        customerId = paymentSource?.card?.attributes?.vault?.customer?.id
    )
}

fun SetupTokenResponse.toCardSetupToken(): CardSetupToken {
    return CardSetupToken(
        id = id,
        customerId = customer.id,
        status = status,
        verificationStatus = paymentSource?.card?.verificationStatus
    )
}

fun PaymentTokenResponse.toCardPaymentToken(): CardPaymentToken {
    return CardPaymentToken(
        id = id,
        customerId = customer.id,
        lastDigits = paymentSource?.card?.lastDigits ?: "",
        cardBrand = paymentSource?.card?.brand ?: ""
    )
}

fun PaymentTokenResponse.toPayPalPaymentToken(): PayPalPaymentToken {
    return PayPalPaymentToken(
        id = id,
        customerId = customer.id
    )
}
