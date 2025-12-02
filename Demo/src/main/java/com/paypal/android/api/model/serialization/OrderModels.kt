package com.paypal.android.api.model.serialization

import com.paypal.android.api.model.OrderIntent
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequestBody(
    val intent: OrderIntent,
    val purchaseUnits: List<PurchaseUnit>,
    val paymentSource: OrderPaymentSource? = null
)

@Serializable
data class PurchaseUnit(
    val amount: Amount
)

@Serializable
data class Amount(
    val currencyCode: String,
    val value: String
)

@Serializable
data class OrderPaymentSource(
    val card: Card? = null,
    val paypal: PayPalPaymentSource? = null
)

@Serializable
data class Card(
    val attributes: CardAttributes
)

@Serializable
data class CardAttributes(
    val vault: Vault
)

@Serializable
data class Vault(
    val storeInVault: String
)

@Serializable
data class PayPalPaymentSource(
    val experienceContext: PayPalOrderExperienceContext
)

@Serializable
data class PayPalOrderExperienceContext(
    val returnUrl: String,
    val cancelUrl: String,
    val nativeApp: NativeApp
)

@Serializable
data class NativeApp(
    val appUrl: String
)
