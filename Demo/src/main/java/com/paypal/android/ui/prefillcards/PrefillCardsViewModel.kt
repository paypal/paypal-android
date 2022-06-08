package com.paypal.android.ui.prefillcards

import androidx.lifecycle.ViewModel
import com.paypal.android.card.Card
import com.paypal.android.data.card.TestCard
import com.paypal.android.data.card.TestCardGroup

class PrefillCardsViewModel : ViewModel() {

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
                    name = "3DS1",
                    card = Card(
                        number = "4000000000000002",
                        expirationMonth = "01",
                        expirationYear = "2026",
                        securityCode = "123"
                    ),
                ),
                TestCard(
                    name = "3DS2",
                    card = Card(
                        number = "4000000000001091",
                        expirationMonth = "01",
                        expirationYear = "2026",
                        securityCode = "123"
                    ),
                ),
            )
        )
    )
}
