package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun MessageView(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(UIConstants.paddingMedium)
    )
}

@Preview
@Composable
fun MessageViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MessageView(message = "This is a message.")
        }
    }
}
