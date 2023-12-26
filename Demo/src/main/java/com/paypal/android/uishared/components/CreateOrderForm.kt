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
import com.paypal.android.utils.UIConstants

@Composable
fun CreateOrderForm(
    orderIntent: OrderIntent = OrderIntent.AUTHORIZE,
    onIntentOptionSelected: (OrderIntent) -> Unit = {},
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        EnumOptionList(
            title = stringResource(id = R.string.intent_title),
            stringArrayResId = R.array.intent_options,
            onOptionSelected = { onIntentOptionSelected(it) },
            selectedOption = orderIntent
        )
    }
}

@Preview
@Composable
fun CreateOrderFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CreateOrderForm()
        }
    }
}
