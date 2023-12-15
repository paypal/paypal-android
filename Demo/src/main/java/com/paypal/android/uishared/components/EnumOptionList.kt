package com.paypal.android.uishared.components

import androidx.annotation.ArrayRes
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.uishared.enums.StoreInVaultOption

// Ref: https://stackoverflow.com/a/51663849
@Composable
inline fun <reified T : Enum<T>> EnumOptionList(
    title: String,
    @ArrayRes stringArrayResId: Int,
    crossinline onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    selectedOption: T,
) {
    val options = stringArrayResource(id = stringArrayResId)

    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseSurface)
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(16.dp)
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
                        .padding(horizontal = 16.dp)
                        .defaultMinSize(minHeight = 50.dp)
                        .selectable(
                            selected = (enumValueOf<T>(option) == selectedOption),
                            onClick = {
                                onOptionSelected(enumValueOf(option))
                            },
                            role = Role.RadioButton
                        )
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .weight(1.0f)
                            .align(Alignment.CenterVertically)
                    )
                    RadioButton(
                        selected = (enumValueOf<T>(option) == selectedOption),
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
        Surface {
            EnumOptionList(
                title = "Fake Title",
                stringArrayResId = R.array.store_in_vault_options,
                onOptionSelected = {},
                selectedOption = StoreInVaultOption.ON_SUCCESS,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
