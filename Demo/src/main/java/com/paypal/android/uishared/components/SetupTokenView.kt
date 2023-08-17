package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.api.model.SetupToken

@Composable
fun SetupTokenView(setupToken: SetupToken) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Setup Token",
                style = MaterialTheme.typography.titleLarge
            )
            PropertyView(name = "ID", value = setupToken.id)
            PropertyView(name = "Customer ID", value = setupToken.customerId)
            PropertyView(name = "Status", value = setupToken.status)
        }
    }
}
