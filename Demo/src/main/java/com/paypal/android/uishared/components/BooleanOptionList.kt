package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

// Ref: https://stackoverflow.com/a/51663849
@Composable
fun BooleanOptionList(
    title: String,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    value: Boolean,
) {
    val options = listOf("NO", "YES")

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
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val isLast = (index == options.lastIndex)
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
                        .selectable(
                            selected = (value && (option == "YES")) || (!value && (option == "NO")),
                            onClick = { onValueChange(option == "YES") },
                            role = Role.RadioButton
                        )
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(UIConstants.paddingMedium)
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                    )
                    RadioButton(
                        selected = (value && (option == "YES")) || (!value && (option == "NO")),
                        onClick = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = UIConstants.paddingMedium)
                    )
                }
                if (!isLast) {
                    Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
                }
            }
        }
    }
}

@Preview
@Composable
fun BooleanOptionListPreview() {
    MaterialTheme {
        Surface {
            BooleanOptionList(
                title = "Fake Title",
                onValueChange = {},
                value = false,
                modifier = Modifier.padding(UIConstants.paddingSmall)
            )
        }
    }
}
