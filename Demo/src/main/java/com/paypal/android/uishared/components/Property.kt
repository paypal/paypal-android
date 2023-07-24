package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Property(name: String, value: String?) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        text = value ?: "UNSET",
        modifier = Modifier.padding(top = 4.dp)
    )
}
