package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.ShippingChangeType
import com.paypal.android.checkout.pojo.ShippingType
import com.paypal.android.checkout.pojo.asNativeCheckout
import com.paypal.android.checkout.pojo.asPaypalCheckout
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class EnumTest {

    @Test
    fun `CurrencyCode has a values in payPalCheckout and native`() {
        CurrencyCode.values().forEach { currencyCode ->
           expectThat(currencyCode.asNativeCheckout).isEqualTo(com.paypal.checkout.createorder.CurrencyCode.valueOf(currencyCode.name))
        }
        com.paypal.checkout.createorder.CurrencyCode.values().forEach { nativeCurrencyCode ->
            expectThat(nativeCurrencyCode.asPaypalCheckout).isEqualTo(CurrencyCode.valueOf(nativeCurrencyCode.name))
        }
    }

    @Test
    fun `UserAction has a values in payPalCheckout and native`() {
        UserAction.values().forEach { userAction ->
            expectThat(userAction.asNativeCheckout).isEqualTo(com.paypal.checkout.createorder.UserAction.valueOf(userAction.name))
        }
        com.paypal.checkout.createorder.UserAction.values().forEach { nativeUserAction->
            expectThat(nativeUserAction.asPaypalCheckout).isEqualTo(UserAction.valueOf(nativeUserAction.name))
        }
    }

    @Test
    fun `OrderIntent has a values in payPalCheckout and native`() {
        OrderIntent.values().forEach { orderIntent ->
            expectThat(orderIntent.asNativeCheckout).isEqualTo(com.paypal.checkout.createorder.OrderIntent.valueOf(orderIntent.name))
        }
        com.paypal.checkout.createorder.OrderIntent.values().forEach { nativeOrderIntent ->
            expectThat(nativeOrderIntent.asPaypalCheckout).isEqualTo(OrderIntent.valueOf(nativeOrderIntent.name))
        }
    }

    @Test
    fun `PaymentButtonIntent has a values in payPalCheckout and native`() {
        PaymentButtonIntent.values().forEach { paymentButtonIntent ->
            expectThat(paymentButtonIntent.asNativeCheckout).isEqualTo(com.paypal.checkout.config.PaymentButtonIntent.valueOf(paymentButtonIntent.name))
        }
        com.paypal.checkout.config.PaymentButtonIntent.values().forEach { nativePaymentButtonIntent ->
            expectThat(nativePaymentButtonIntent.asPaypalCheckout).isEqualTo(PaymentButtonIntent.valueOf(nativePaymentButtonIntent.name))
        }
    }

    @Test
    fun `ShippingType has a values in payPalCheckout and native`() {
        ShippingType.values().forEach { shippingType ->
            expectThat(shippingType.asNativeCheckout).isEqualTo(com.paypal.checkout.createorder.ShippingType.valueOf(shippingType.name))
        }
        com.paypal.checkout.createorder.ShippingType.values().forEach { nativeShippingType ->
            expectThat(nativeShippingType.asPaypalCheckout).isEqualTo(ShippingType.valueOf(nativeShippingType.name))
        }
    }

    @Test
    fun `ShippingChangeType has a values in payPalCheckout and native`() {
        ShippingChangeType.values().forEach { shippingChangeType ->
            expectThat(shippingChangeType.asNativeCheckout).isEqualTo(com.paypal.checkout.shipping.ShippingChangeType.valueOf(shippingChangeType.name))
        }
        com.paypal.checkout.shipping.ShippingChangeType.values().forEach { nativeShippingChangeType->
            expectThat(nativeShippingChangeType.asPaypalCheckout).isEqualTo(ShippingChangeType.valueOf(nativeShippingChangeType.name))
        }
    }
}