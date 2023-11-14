package com.paypal.android.ui.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.ui.WireframeButton
import com.paypal.android.uishared.components.CardForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSetupTokenWithCardForm(
    uiState: VaultCardUiState,
    onCardNumberChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onSecurityCodeChange: (String) -> Unit,
    onUseTestCardClick: () -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Vault Card",
                style = MaterialTheme.typography.headlineSmall
            )
            CardForm(
                cardNumber = uiState.cardNumber,
                expirationDate = uiState.cardExpirationDate,
                securityCode = uiState.cardSecurityCode,
                onCardNumberChange = { onCardNumberChange(it) },
                onExpirationDateChange = { onExpirationDateChange(it) },
                onSecurityCodeChange = { onSecurityCodeChange(it) },
                onUseTestCardClick = { onUseTestCardClick() }
            )
            Spacer(modifier = Modifier.size(8.dp))
            WireframeButton(
                text = "Vault Card",
                isLoading = uiState.isUpdateSetupTokenLoading,
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
