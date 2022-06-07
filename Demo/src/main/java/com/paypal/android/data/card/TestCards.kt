package com.paypal.android.data.card

import com.paypal.android.card.Card

object TestCards {

    enum class Group {
        CARD, CARD_3DS
    }

    var numGroups: Int = Group.values().size

    private val cards: Map<Group, List<PrefillCard>> = mapOf(
        Group.CARD to listOf(
            PrefillCard(
                name = "Visa",
                card = Card(
                    number = "4111111111111111",
                    expirationMonth = "01",
                    expirationYear = "2026",
                    securityCode = "123"
                )
            ),
            PrefillCard(
                name = "3DS2",
                card = Card(
                    number = "4000000000001091",
                    expirationMonth = "01",
                    expirationYear = "2026",
                    securityCode = "123"
                ),
            ),
            PrefillCard(
                name = "3DS1",
                card = Card(
                    number = "4000000000000002",
                    expirationMonth = "01",
                    expirationYear = "2026",
                    securityCode = "123"
                ),
            ),
            PrefillCard(
                name = "New Visa",
                card = Card(
                    number = "4032035809742661",
                    expirationMonth = "09",
                    expirationYear = "2026",
                    securityCode = "655"
                )
            )
        ),
        Group.CARD_3DS to listOf(

        )
    )

    fun cardsForGroup(group: Group): List<PrefillCard> {
        return cards[group]!!
    }
}