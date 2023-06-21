package com.paypal.android.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@ExperimentalMaterial3Api
@Composable
fun WireframeOptionDropDown(
    hint: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    modifier: Modifier,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    // Ref: https://alexzh.com/jetpack-compose-dropdownmenu/
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            label = { Text(hint) },
            readOnly = true,
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = {}) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text(text = item) }, onClick = {
                    onValueChange(item)
                })
            }
        }
    }
}