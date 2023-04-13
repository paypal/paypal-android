package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.model.PurchaseUnit

data class OrderRequest(val intent: OrderIntent, val purchaseUnits: List<PurchaseUnit>?)
