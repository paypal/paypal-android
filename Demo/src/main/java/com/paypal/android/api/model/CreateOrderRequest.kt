package com.paypal.android.api.model

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    val intent: String?,
    @SerializedName("purchase_units")
    val purchaseUnit: List<PurchaseUnit>?,
    val payee: Payee?,
) {
    @SerializedName("application_context")
    var applicationContext: ApplicationContext? = null
}
