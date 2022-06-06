package com.paypal.android.card

import com.paypal.android.card.model.PurchaseUnit

data class OrderRequest(val intent: OrderIntent, val purchaseUnits: List<PurchaseUnit>?)
