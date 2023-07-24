package com.paypal.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Ref: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.RadioButtonColors,androidx.compose.foundation.interaction.MutableInteractionSource)

@Composable
fun OptionList(
    title: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedOption: String = "",
) {
    Column(modifier) {
        Text(
            text = title,
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Card(
            modifier = Modifier.selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val isLast = (index == options.lastIndex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .defaultMinSize(minHeight = 50.dp)
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = { onOptionSelected(option) },
                            role = Role.RadioButton
                        )
                ) {
                    Text(
                        text = option,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                    )
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if (!isLast) {
                    Divider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Preview
@Composable
fun OptionListPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            OptionList(
                title = "Fake Title",
                options = listOf("One", "Two", "Three"),
                onOptionSelected = {},
                selectedOption = "One",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
