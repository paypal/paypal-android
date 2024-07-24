package com.paypal.android.ui.venmocheckout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.corepayments.features.eligibility.EligibilityResult
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.utils.UIConstants

@Composable
fun EligibilityResultView(result: EligibilityResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(UIConstants.paddingMedium)
    ) {
        val isVenmoEligible = if (result.isVenmoEligible) "YES" else "NO"
        PropertyView(name = "Is Venmo Eligible?", value = isVenmoEligible)
    }
}

@Preview
@Composable
fun EligibilityResultViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            val result = EligibilityResult(
                isVenmoEligible = true,
                isCardEligible = true,
                isPayPalEligible = true,
                isPayLaterEligible = true,
                isCreditEligible = true
            )
            EligibilityResultView(result)
        }
    }
}
