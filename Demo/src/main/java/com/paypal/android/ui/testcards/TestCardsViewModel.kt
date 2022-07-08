package com.paypal.android.ui.testcards

import androidx.lifecycle.ViewModel
import com.paypal.android.card.Card
import com.paypal.android.data.card.TestCard
import com.paypal.android.data.card.TestCardGroup

class TestCardsViewModel : ViewModel() {

    val testCardGroups = listOf(
        TestCardGroup(
            name = "Cards",
            cards = listOf(
                TestCard(
                    name = "Visa",
                    card = Card(
                        number = "4111111111111111",
                        expirationMonth = "01",
                        expirationYear = "2026",
                        securityCode = "123"
                    )
                ),
                TestCard(
                    name = "New Visa",
                    card = Card(
                        number = "4032035809742661",
                        expirationMonth = "09",
                        expirationYear = "2026",
                        securityCode = "655"
                    )
                ),
            )
        ),
        TestCardGroup(
            name = "Cards with 3DS",
            cards = listOf(
                TestCard(
                    name = "3DS Successful Auth",
                    card = Card(
                        number = "4000000000000002",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Failed Signature",
                    card = Card(
                        number = "4000000000000010",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Failed Authentication",
                    card = Card(
                        number = "4000000000000028",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Passive Authentication",
                    card = Card(
                        number = "4000000000000101",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Transaction Timeout",
                    card = Card(
                        number = "4000000000000044",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Not Enrolled",
                    card = Card(
                        number = "4000000000000051",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Auth System Unavailable",
                    card = Card(
                        number = "4000000000000069",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Auth Error",
                    card = Card(
                        number = "4000000000000093",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Auth Unavailable",
                    card = Card(
                        number = "4000000000000036",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Merchant Bypassed Auth",
                    card = Card(
                        number = "4000990000000004",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS Merchant Inactive",
                    card = Card(
                        number = "4000000000000077",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS cmpi_lookup Error",
                    card = Card(
                        number = "4000000000000085",
                        expirationMonth = "01",
                        expirationYear = "2023",
                        securityCode = "123"
                    ),
                ),
            )
        )
    )
}
