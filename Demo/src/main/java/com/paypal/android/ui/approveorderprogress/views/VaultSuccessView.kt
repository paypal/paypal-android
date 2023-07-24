package com.paypal.android.ui.approveorderprogress.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.uishared.components.PropertyView

@Composable
fun VaultSuccessView(vaultResult: VaultResult) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Vault Result",
                style = MaterialTheme.typography.titleLarge,
            )
            PropertyView(name = "Status", value = vaultResult.status)
            PropertyView(name = "Setup Token ID", value = vaultResult.setupTokenId)
            PropertyView(name = "Customer ID", value = vaultResult.customerId)
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@Preview
@Composable
fun VaultSuccessViewPreview() {
    val vaultResult = VaultResult("fake-status", "fake-setup-token-id", "fake-customer-id")
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            VaultSuccessView(vaultResult)
        }
    }
}
