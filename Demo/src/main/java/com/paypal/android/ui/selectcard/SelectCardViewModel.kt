package com.paypal.android.ui.selectcard

import androidx.lifecycle.ViewModel
import com.paypal.android.models.TestCard
import java.util.Calendar

class SelectCardViewModel : ViewModel() {
    companion object {
        // 2 years into the future of the current year
        val validExpirationYear = "${Calendar.getInstance().get(Calendar.YEAR) + 2}"
    }

    val verifiedTestCards = listOf(
        TestCard.VISA_VAULT_WITH_PURCHASE_NO_3DS
    )

    val nonThreeDSCards = listOf(
        TestCard.VISA_NO_3DS_1,
        TestCard.VISA_NO_3DS_2
    )

    val threeDSCards = listOf(
        TestCard.VISA_3DS_SUCCESSFUL_AUTH,
        TestCard.VISA_3DS_FAILED_SIGNATURE,
        TestCard.VISA_3DS_FAILED_AUTHENTICATION,
        TestCard.VISA_3DS_PASSIVE_AUTHENTICATION,
        TestCard.VISA_3DS_TRANSACTION_TIMEOUT,
        TestCard.VISA_3DS_NOT_ENROLLED,
        TestCard.VISA_3DS_AUTH_SYSTEM_UNAVAILABLE,
        TestCard.VISA_3DS_AUTH_ERROR,
        TestCard.VISA_3DS_AUTH_UNAVAILABLE,
        TestCard.VISA_3DS_MERCHANT_BYPASSED_AUTH,
        TestCard.VISA_3DS_MERCHANT_INACTIVE,
        TestCard.VISA_3DS_CMPI_LOOKUP_ERROR
    )
}
