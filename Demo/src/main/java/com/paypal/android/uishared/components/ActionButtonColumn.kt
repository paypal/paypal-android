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
import androidx.compose.ui.unit.dp
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.utils.UIConstants

private val successGreen = Color(color = 0xff007f5f)

@Composable
fun <S, E> ActionButtonColumn(
    defaultTitle: String,
    successTitle: String,
    state: ActionState<S, E>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val isLoading = state is ActionState.Loading
    // TODO: use material themed color for success
    val buttonBackground = when (state) {
        is ActionState.Loading, is ActionState.Idle ->
            MaterialTheme.colorScheme.inverseSurface

        is ActionState.Failure -> MaterialTheme.colorScheme.errorContainer
        is ActionState.Success -> successGreen
    }

    val buttonForeground = when (state) {
        is ActionState.Loading, is ActionState.Idle ->
            MaterialTheme.colorScheme.inverseOnSurface

        is ActionState.Failure -> MaterialTheme.colorScheme.onErrorContainer
        is ActionState.Success -> Color.White
    }

    Card(
        modifier = modifier
    ) {
        Button(
            onClick = {
                if (state is ActionState.Idle) {
                    onClick()
                }
            },
            // force button to rectangle to allow Card parent to perform corner radius clipping
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackground,
                contentColor = buttonForeground
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box {
                // in loading state, blend text in with background and show loading indicator
                Text(
                    text = if (state is ActionState.Success) successTitle else defaultTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = UIConstants.paddingSmall)
                        .alpha(if (isLoading) 0.0f else 1.0f)
                )
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(28.dp)
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
fun StatefulActionButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                ActionButtonColumn(
                    defaultTitle = "Fake Default Title",
                    successTitle = "Fake Success Title",
                    state = ActionState.Idle,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UIConstants.paddingMedium)
                )
            }
        }
    }
}
