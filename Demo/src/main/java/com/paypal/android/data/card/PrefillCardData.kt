package com.paypal.android.data.card

import com.paypal.android.card.Card

object PrefillCardData {

    val cards by lazy {
        listOf(
            "Visa" to Card(
                number = "4111111111111111",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            ),
            "3DS2" to Card(
                number = "4000000000001091",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            ),
            "3DS1" to Card(
                number = "4000000000000002",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            )
        )
    }
}
