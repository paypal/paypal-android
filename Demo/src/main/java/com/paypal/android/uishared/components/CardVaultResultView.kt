package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.ui.approveorder.SetupTokenInfo
import com.paypal.android.utils.UIConstants

@Composable
fun CardVaultResultView(result: SetupTokenInfo) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Setup Token ID", value = result.setupTokenId)
        PropertyView(name = "Status", value = result.status)
        val didAttemptText = if (result.didAttemptThreeDSecureAuthentication) "YES" else "NO"
        PropertyView(name = "Did Attempt 3DS Authentication", value = didAttemptText)
    }
}
