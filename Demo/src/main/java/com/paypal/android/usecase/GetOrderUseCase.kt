package com.paypal.android.usecase

import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.OrderIntent
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.Order
import com.paypal.checkout.order.PurchaseUnit

class GetOrderUseCase {

    operator fun invoke() = Order(
        OrderIntent.CAPTURE,
        AppContext(),
        listOf(PurchaseUnit(amount = Amount(CurrencyCode.USD, "60")))
    )
}
