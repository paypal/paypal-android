package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun BooleanOptionList(
    title: String,
    selectedOption: Boolean,
    onSelectedOptionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("NO", "YES")
    val selectedOptionString = if (selectedOption) "YES" else "NO"
    OptionList(
        title = title,
        options = options,
        selectedOption = selectedOptionString,
        onSelectedOptionChange = { value -> onSelectedOptionChange(value == "YES") },
        modifier = modifier
    )
}

@Preview
@Composable
fun BooleanOptionListPreview() {
    MaterialTheme {
        Surface {
            BooleanOptionList(
                title = "Fake Title",
                onSelectedOptionChange = {},
                selectedOption = true,
                modifier = Modifier.padding(UIConstants.paddingSmall)
            )
        }
    }
}
