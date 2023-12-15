package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StepContainer(
    stepNumber: Int,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .size(60.dp)
            ) {
                Text(
                    text = "$stepNumber",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1.0f)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red)
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            // invisible box to preserve step content alignment
            Box(
                modifier = Modifier
                    .alpha(0f)
                    .size(60.dp)
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1.0f)
            ) {
                content()
            }
        }
    }
}

@Preview
@Composable
fun PayPalWebViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            StepContainer(99, "Title 99") {
                Column {
                    Text("More")
                    Text("Stuff")
                    Text("For")
                    Text("Example")
                }
            }
        }
    }
}
