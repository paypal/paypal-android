package com.paypal.android.cardpayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
/**
 * A purchase unit for an order. If an order takes multiple purchase units,
 * each one must contain a reference id.
 */
@Serializable
internal data class PurchaseUnit(
    @SerialName("reference_id")
    val referenceId: String?,
    @SerialName("amount")
    val amount: Amount? = null,
    @SerialName("payee")
    val payee: Payee? = null
)
