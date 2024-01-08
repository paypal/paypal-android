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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun StepHeader(stepNumber: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = CircleShape
                )
                .size(UIConstants.stepNumberBackgroundSize)
        ) {
            Text(
                text = "$stepNumber",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.headlineSmall.copy(
                    // Ref: https://stackoverflow.com/a/73947453
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
        Column(
            modifier = Modifier
                .padding(start = UIConstants.paddingSmall)
                .weight(1.0f)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun PayPalWebViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            StepHeader(stepNumber = 99, title = "Title 99")
        }
    }
}
