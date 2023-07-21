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

@Composable
fun VaultSuccessView(result: VaultResult) {
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
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = result.status,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Setup Token ID",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = result.setupTokenId,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Customer ID",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = result.customerId,
                modifier = Modifier.padding(top = 4.dp)
            )
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
