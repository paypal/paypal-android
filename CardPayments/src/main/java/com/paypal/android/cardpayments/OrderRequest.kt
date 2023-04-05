package com.paypal.android.cardpayments

import com.paypal.android.corepayments.models.OrderIntent
import com.paypal.android.corepayments.models.PurchaseUnit

data class OrderRequest(val intent: OrderIntent, val purchaseUnits: List<PurchaseUnit>?)
