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
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.utils.UIConstants

// Ref: https://stackoverflow.com/a/51663849
@Composable
inline fun <reified T : Enum<T>> EnumOptionList(
    title: String,
    @ArrayRes stringArrayResId: Int,
    crossinline onSelectedOptionChange: (T) -> Unit,
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
                            selected = (enumValueOf<T>(option) == selectedOption),
                            onClick = {
                                onSelectedOptionChange(enumValueOf(option))
                            },
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
                        selected = (enumValueOf<T>(option) == selectedOption),
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
fun EnumOptionListPreview() {
    MaterialTheme {
        Surface {
            EnumOptionList(
                title = "Fake Title",
                stringArrayResId = R.array.store_in_vault_options,
                onSelectedOptionChange = {},
                selectedOption = StoreInVaultOption.ON_SUCCESS,
                modifier = Modifier.padding(UIConstants.paddingSmall)
            )
        }
    }
}
