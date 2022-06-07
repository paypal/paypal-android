package com.paypal.android.data.card

import com.paypal.android.card.Card

object TestCards {

    enum class Group {
        CARD, CARD_3DS
    }

    var numGroups: Int = Group.values().size

    var cards: Map<String, PrefillCard> = mapOf(
        "Visa" to PrefillCard(
            name = "Visa",
            card = Card(
                number = "4111111111111111",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            )
        ),
        "New Visa" to PrefillCard(
            name = "New Visa",
            card = Card(
                number = "4032035809742661",
                expirationMonth = "09",
                expirationYear = "2026",
                securityCode = "655"
            )
        ),
        "3DS1" to PrefillCard(
            name = "3DS1",
            card = Card(
                number = "4000000000000002",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            ),
        ),
        "3DS2" to PrefillCard(
            name = "3DS2",
            card = Card(
                number = "4000000000001091",
                expirationMonth = "01",
                expirationYear = "2026",
                securityCode = "123"
            ),
        ),
    )

    private val cardGroups: Map<Group, List<String>> = mapOf(
        Group.CARD to listOf(
            "Visa",
            "New Visa"
        ),
        Group.CARD_3DS to listOf(
            "3DS1",
            "3DS2",
        )
    )
}