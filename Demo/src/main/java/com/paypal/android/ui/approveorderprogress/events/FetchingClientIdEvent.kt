package com.paypal.android.ui.approveorderprogress.events

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
import com.paypal.android.ui.approveorderprogress.ApproveOrderEvent

class FetchingClientIdEvent : ApproveOrderEvent {

    @Composable
    override fun AsComposable() {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Fetching Client ID...",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }

    @Preview
    @Composable
    fun PreviewAsComposable() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxWidth()) {
                AsComposable()
            }
        }
    }
}

