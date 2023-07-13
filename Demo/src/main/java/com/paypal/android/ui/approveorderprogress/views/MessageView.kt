package com.paypal.android.ui.approveorderprogress.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MessageView(message: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(16.dp)
        )
    }
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
