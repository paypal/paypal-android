package com.paypal.android.models

import android.os.Parcelable
import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.approveorder.CardFormatter
import com.paypal.android.ui.selectcard.SelectCardViewModel.Companion.validExpirationYear
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestCard(val id: String, val name: String, val card: Card) : Parcelable {

    companion object {
        val VISA_VAULT_WITH_PURCHASE_NO_3DS = TestCard(
            id = "vault_with_purchase_no_3DS",
            name = "🇺🇸 Vault with Purchase (no 3DS)",
            card = Card(
                number = "4000000000000002",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_NO_3DS_1 = TestCard(
            id = "visa_no_3DS_1",
            name = "Visa",
            card = Card(
                number = "4111111111111111",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            )
        )

        val VISA_NO_3DS_2 = TestCard(
            id = "visa_no_3DS_2",
            name = "New Visa",
            card = Card(
                number = "4032035809742661",
                expirationMonth = "09",
                expirationYear = validExpirationYear,
                securityCode = "655"
            )
        )

        val VISA_3DS_SUCCESSFUL_AUTH = TestCard(
            id = "visa_3DS_successful_auth",
            name = "3DS Successful Auth",
            card = Card(
                number = "4000000000000002",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_FAILED_SIGNATURE = TestCard(
            id = "visa_3DS_failed_signature",
            name = "3DS Failed Signature",
            card = Card(
                number = "4000000000000010",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_FAILED_AUTHENTICATION = TestCard(
            id = "visa_3DS_failed_authentication",
            name = "3DS Failed Authentication",
            card = Card(
                number = "4000000000000028",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_PASSIVE_AUTHENTICATION = TestCard(
            id = "visa_3DS_passive_authentication",
            name = "3DS Passive Authentication",
            card = Card(
                number = "4000000000000101",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_TRANSACTION_TIMEOUT = TestCard(
            id = "visa_3DS_transaction_timeout",
            name = "3DS Transaction Timeout",
            card = Card(
                number = "4000000000000044",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_NOT_ENROLLED = TestCard(
            id = "visa_3DS_not_enrolled",
            name = "3DS Not Enrolled",
            card = Card(
                number = "4000000000000051",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_AUTH_SYSTEM_UNAVAILABLE = TestCard(
            id = "visa_3DS_auth_system_unavailable",
            name = "3DS Auth System Unavailable",
            card = Card(
                number = "4000000000000069",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_AUTH_ERROR = TestCard(
            id = "visa_3DS_auth_error",
            name = "3DS Auth Error",
            card = Card(
                number = "4000000000000093",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_AUTH_UNAVAILABLE = TestCard(
            id = "visa_3DS_auth_unavailable",
            name = "3DS Auth Unavailable",
            card = Card(
                number = "4000000000000036",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_MERCHANT_BYPASSED_AUTH = TestCard(
            id = "visa_3DS_merchant_bypassed_auth",
            name = "3DS Merchant Bypassed Auth",
            card = Card(
                number = "4000990000000004",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_MERCHANT_INACTIVE = TestCard(
            id = "visa_3DS_merchant_inactive",
            name = "3DS Merchant Inactive",
            card = Card(
                number = "4000000000000077",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        val VISA_3DS_CMPI_LOOKUP_ERROR = TestCard(
            id = "visa_3DS_cmpi_lookup_error",
            name = "3DS cmpi_lookup Error",
            card = Card(
                number = "4000000000000085",
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )

        private val testCardMapById = mapOf(
            VISA_VAULT_WITH_PURCHASE_NO_3DS.id to VISA_VAULT_WITH_PURCHASE_NO_3DS,
            VISA_NO_3DS_1.id to VISA_NO_3DS_1,
            VISA_NO_3DS_2.id to VISA_NO_3DS_2,
            VISA_3DS_SUCCESSFUL_AUTH.id to VISA_3DS_SUCCESSFUL_AUTH,
            VISA_3DS_FAILED_SIGNATURE.id to VISA_3DS_FAILED_SIGNATURE,
            VISA_3DS_FAILED_AUTHENTICATION.id to VISA_3DS_FAILED_AUTHENTICATION,
            VISA_3DS_PASSIVE_AUTHENTICATION.id to VISA_3DS_PASSIVE_AUTHENTICATION,
            VISA_3DS_TRANSACTION_TIMEOUT.id to VISA_3DS_TRANSACTION_TIMEOUT,
            VISA_3DS_NOT_ENROLLED.id to VISA_3DS_NOT_ENROLLED,
            VISA_3DS_AUTH_SYSTEM_UNAVAILABLE.id to VISA_3DS_AUTH_SYSTEM_UNAVAILABLE,
            VISA_3DS_AUTH_ERROR.id to VISA_3DS_AUTH_ERROR,
            VISA_3DS_AUTH_UNAVAILABLE.id to VISA_3DS_AUTH_UNAVAILABLE,
            VISA_3DS_MERCHANT_BYPASSED_AUTH.id to VISA_3DS_MERCHANT_BYPASSED_AUTH,
            VISA_3DS_MERCHANT_INACTIVE.id to VISA_3DS_MERCHANT_INACTIVE,
            VISA_3DS_CMPI_LOOKUP_ERROR.id to VISA_3DS_CMPI_LOOKUP_ERROR,
        )

        fun byId(testCardId: String): TestCard? {
            return testCardMapById[testCardId]
        }
    }

    @IgnoredOnParcel
    val formattedCardNumber: String = CardFormatter.formatCardNumber(card.number)
}
