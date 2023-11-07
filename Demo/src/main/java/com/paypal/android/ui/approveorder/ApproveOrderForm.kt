package com.paypal.android.ui.approveorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.stringResourceListOf
import com.paypal.android.uishared.components.CardForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveOrderForm(
    uiState: ApproveOrderUiState,
    onUseTestCardClick: () -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onSecurityCodeChange: (String) -> Unit,
    onSCAOptionSelected: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Approve Order with Card",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            CardForm(
                cardNumber = uiState.cardNumber,
                expirationDate = uiState.cardExpirationDate,
                securityCode = uiState.cardSecurityCode,
                onCardNumberChange = { onCardNumberChange(it) },
                onExpirationDateChange = { onExpirationDateChange(it) },
                onSecurityCodeChange = { onSecurityCodeChange(it) },
                onUseTestCardClick = { onUseTestCardClick() }
            )
            Spacer(modifier = Modifier.size(16.dp))
            OptionList(
                title = stringResource(id = R.string.sca_title),
                options = stringResourceListOf(R.string.sca_always, R.string.sca_when_required),
                selectedOption = uiState.scaOption,
                onOptionSelected = { onSCAOptionSelected(it) }
            )
            Spacer(modifier = Modifier.size(16.dp))
            WireframeButton(
                text = "APPROVE ORDER",
                isLoading = uiState.isApproveOrderLoading,
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun ApproveOrderFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ApproveOrderForm(
                uiState = ApproveOrderUiState(),
                onCardNumberChange = {},
                onExpirationDateChange = {},
                onSecurityCodeChange = {},
                onSCAOptionSelected = {},
                onUseTestCardClick = {},
                onSubmit = {}
            )
        }
    }
}
