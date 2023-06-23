package com.paypal.android.ui.selectcard

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.Card
import com.paypal.android.models.TestCard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar

@HiltViewModel
class SelectCardViewModel : ViewModel() {
    companion object {
        // 2 years into the future of the current year
        val validExpirationYear = "${Calendar.getInstance().get(Calendar.YEAR) + 2}"
    }

    val nonThreeDSCards = listOf(
        TestCard(
            name = "Visa",
            card = Card(
                number = "4111111111111111",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            )
        ),
        TestCard(
            name = "New Visa",
            card = Card(
                number = "4032035809742661",
                expirationMonth = "09",
                expirationYear = validExpirationYear,
                securityCode = "655"
            )
        ),
    )

    val threeDSCards = listOf(
        TestCard(
            name = "3DS Successful Auth",
            card = Card(
                number = "4000000000000002",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Failed Signature",
            card = Card(
                number = "4000000000000010",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Failed Authentication",
            card = Card(
                number = "4000000000000028",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Passive Authentication",
            card = Card(
                number = "4000000000000101",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Transaction Timeout",
            card = Card(
                number = "4000000000000044",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Not Enrolled",
            card = Card(
                number = "4000000000000051",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Auth System Unavailable",
            card = Card(
                number = "4000000000000069",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Auth Error",
            card = Card(
                number = "4000000000000093",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Auth Unavailable",
            card = Card(
                number = "4000000000000036",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Merchant Bypassed Auth",
            card = Card(
                number = "4000990000000004",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS Merchant Inactive",
            card = Card(
                number = "4000000000000077",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
        TestCard(
            name = "3DS cmpi_lookup Error",
            card = Card(
                number = "4000000000000085",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        ),
    )
}
