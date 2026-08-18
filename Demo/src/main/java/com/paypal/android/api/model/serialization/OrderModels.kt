package com.paypal.android.api.model.serialization

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.model.PaymentMethodSelected
import com.paypal.android.api.model.UserActionSelected
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
    val storeInVault: String,
    val usageType: String? = null,
    val customerType: String? = null
)

@Serializable
data class PayPalPaymentSource(
    val usageType: String? = null,
    val emailAddress: String? = null,
    val experienceContext: PayPalOrderExperienceContext? = null,
    val usagePattern: String? = null,
    val billingPlan: String? = null,
    val attributes: PayPalAttributes? = null,
    val token: String? = null
)

@Serializable
data class PayPalAttributes(
    val vault: Vault
)

@Serializable
data class PayPalOrderExperienceContext(
    val returnUrl: String,
    val cancelUrl: String,
    val paymentMethodSelected: PaymentMethodSelected? = null,
    val userAction: UserActionSelected? = null,
    val appSwitchContext: AppSwitchContext? = null
)

@Serializable
data class AppSwitchContext(
    val nativeApp: NativeApp
)

@Serializable
data class NativeApp(
    val appUrl: String
)
