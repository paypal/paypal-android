package com.paypal.android.data.card

import com.paypal.android.card.Card

object PrefillCardData {

    val cards by lazy {
        listOf(
            "Visa" to Card(
                cardNumber = "4111111111111111",
                expirationDate = "01/26",
                securityCode = "123"
            ),
            "3DS2" to Card(
                cardNumber = "4000000000001091",
                expirationDate = "01/26",
                securityCode = "123"
            ),
            "3DS1" to Card(
                cardNumber = "4000000000000002",
                expirationDate = "01/26",
                securityCode = "123"
            )
        )
    }
}
