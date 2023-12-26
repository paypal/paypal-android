package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.utils.UIConstants

@Composable
fun PropertyView(name: String, value: String?) {
    Column(
        verticalArrangement = UIConstants.spacingExtraSmall
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(text = value ?: "UNSET")
    }
}
