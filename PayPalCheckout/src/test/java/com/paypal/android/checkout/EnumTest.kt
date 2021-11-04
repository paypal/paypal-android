package com.paypal.android.checkout

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class EnumTest {

    @Test
    fun `CurrencyCode has a values in payPalCheckout and native`() {
        CurrencyCode.values().forEach { currencyCode ->
            expectThat(currencyCode.asNativeCheckout).isEqualTo(
                com.paypal.checkout.createorder.CurrencyCode.valueOf(
                    currencyCode.name
                )
            )
        }
        com.paypal.checkout.createorder.CurrencyCode.values().forEach { nativeCurrencyCode ->
            expectThat(nativeCurrencyCode.asPaypalCheckout).isEqualTo(
                CurrencyCode.valueOf(
                    nativeCurrencyCode.name
                )
            )
        }
    }
}
