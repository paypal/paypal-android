package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun LabeledSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseSurface)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(UIConstants.paddingMedium)
                    .fillMaxWidth()
            )
        }
        Column {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (checked) "On" else "Off",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(UIConstants.paddingMedium)
                        .weight(1.0f)
                        .align(Alignment.CenterVertically)
                )
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = UIConstants.paddingMedium)
                )
            }
        }
    }
}

@Preview
@Composable
fun LabeledSwitchPreview() {
    MaterialTheme {
        Surface {
            LabeledSwitch(
                title = "Should Vault",
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.padding(UIConstants.paddingSmall)
            )
        }
    }
}
