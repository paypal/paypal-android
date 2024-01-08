package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.utils.UIConstants

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.inverseSurface,
    contentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            // force button to rectangle to allow Card parent to perform corner radius clipping
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box {
                Text(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = UIConstants.paddingMedium)
                        .alpha(if (isLoading) 0.0f else 1.0f)
                )
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(UIConstants.progressIndicatorSize)
                        .align(Alignment.Center)
                        .alpha(if (isLoading) 1.0f else 0.0f)
                )
            }
        }

        // optional content
        content()
    }
}

@Preview
@Composable
fun WireframeButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                ActionButton(
                    text = "Fake Text",
                    isLoading = false,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UIConstants.paddingMedium)
                )
            }
        }
    }
}
