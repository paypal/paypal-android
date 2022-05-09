package com.paypal.android.card

data class OrderRequest(
    val intent: OrderIntent,
    val purchaseUnits: List<PurchaseUnit>?
    )