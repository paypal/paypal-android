package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton

@Composable
fun CreateOrderForm(
    title: String,
    orderIntent: OrderIntent = OrderIntent.AUTHORIZE,
    isLoading: Boolean = false,
    onIntentOptionSelected: (OrderIntent) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    val captureValue = stringResource(id = R.string.intent_capture)
    val authorizeValue = stringResource(id = R.string.intent_authorize)
    val selectedOrderIntent = when (orderIntent) {
        OrderIntent.CAPTURE -> captureValue
        OrderIntent.AUTHORIZE -> authorizeValue
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.size(16.dp))
            OptionList(
                title = stringResource(id = R.string.intent_title),
                options = listOf(authorizeValue, captureValue),
                selectedOption = selectedOrderIntent,
                onOptionSelected = { option ->
                    val newOrderIntent = when (option) {
                        captureValue -> OrderIntent.CAPTURE
                        authorizeValue -> OrderIntent.AUTHORIZE
                        else -> null
                    }
                    newOrderIntent?.let { onIntentOptionSelected(it) }
                }
            )
            Spacer(modifier = Modifier.size(16.dp))
            WireframeButton(
                text = "Create Order",
                isLoading = isLoading,
                onClick = { onSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun CreateOrderFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CreateOrderWithVaultOptionForm(title = "Sample Title")
        }
    }
}
