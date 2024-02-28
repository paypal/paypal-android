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

@Composable
fun OptionList(
    title: String,
    options: List<String>,
    onSelectedOptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedOption: String = "",
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
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val isLast = (index == options.lastIndex)
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = { onSelectedOptionChange(option) },
                            role = Role.RadioButton,
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
                        selected = (option == selectedOption),
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
fun OptionListPreview() {
    MaterialTheme {
        Surface {
            OptionList(
                title = "Fake Title",
                options = listOf("One", "Two", "Three"),
                onSelectedOptionChange = {},
                selectedOption = "One",
                modifier = Modifier.padding(UIConstants.paddingSmall)
            )
        }
    }
}
