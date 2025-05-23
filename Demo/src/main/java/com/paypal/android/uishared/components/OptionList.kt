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
        OptionListTitle(text = title)
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                OptionListItem(
                    text = option,
                    isSelected = (option == selectedOption),
                    onClick = { onSelectedOptionChange(option) }
                )
                val isLastOption = (index == options.lastIndex)
                if (!isLastOption) {
                    Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
                }
            }
        }
    }
}

@Composable
fun OptionListTitle(text: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.inverseSurface)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(UIConstants.paddingMedium)
                .fillMaxWidth()
        )
    }
}

@Composable
fun OptionListItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(UIConstants.paddingMedium)
                .weight(1.0f)
                .align(Alignment.CenterVertically)
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = UIConstants.paddingMedium)
        )
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
