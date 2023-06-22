package com.paypal.android.ui.approveorderprogress.events

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paypal.android.uishared.events.ComposableEvent

class MessageEvent(private val message: String) : ComposableEvent {

    @Composable
    override fun AsComposable() {
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
}

