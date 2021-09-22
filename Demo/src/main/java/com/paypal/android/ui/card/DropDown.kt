package com.paypal.android.ui.card

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DropDown(
    items: List<String>,
    defaultValue: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = remember { mutableStateOf(false) }
    val selectedText = remember { mutableStateOf(defaultValue) }

    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    expanded.value = !expanded.value
                }
                .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                selectedText.value,
                modifier = Modifier.weight(1F)
            )
            Icon(Icons.Filled.ArrowDropDown, "contentDescription")
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { label ->
                DropdownMenuItem(onClick = {
                    selectedText.value = label
                    expanded.value = false
                    onValueChanged(label)
                }) {
                    Text(text = label)
                }
            }
        }
    }
}