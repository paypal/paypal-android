package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun InfoColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseSurface),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(UIConstants.paddingMedium)
                    .fillMaxWidth()
            )
        }
        Column {
            content()
        }
    }
}

@Preview
@Composable
fun InfoColumnPreview() {
    MaterialTheme {
        Column {
            InfoColumn(
                title = "Sample Title"
            ) {
                Text(
                    text = "Sample Text",
                    modifier = Modifier.padding(UIConstants.paddingLarge)
                )
            }
        }
    }
}
