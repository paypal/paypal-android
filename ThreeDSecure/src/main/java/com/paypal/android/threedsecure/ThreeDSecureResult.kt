package com.paypal.android.threedsecure

import com.paypal.android.core.OrderStatus

data class ThreeDSecureResult(val orderID: String, val status: OrderStatus, val payerActionHref: String)
