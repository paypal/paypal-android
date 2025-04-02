package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.paypal.android.paymentbuttons.CardButton
import com.paypal.android.paymentbuttons.CardButtonLabel
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PaymentButtonSize
import com.paypal.android.uishared.enums.DemoPaymentButtonType
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.UIConstants

@Composable
fun <S, E> ActionPaymentButtonColumn(
    type: DemoPaymentButtonType,
    state: ActionState<S, E>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (CompletedActionState<S, E>) -> Unit = {},
) {
    Card(
        modifier = modifier
    ) {
        DemoPaymentButton(
            type = type,
            onClick = {
                if (state is ActionState.Idle) {
                    onClick()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // optional content
        val completedState = when (state) {
            is ActionState.Success -> CompletedActionState.Success(state.value)
            is ActionState.Failure -> CompletedActionState.Failure(state.value)
            else -> null
        }
        completedState?.let {
            content(completedState)
        }
    }
}

@Composable
fun DemoPaymentButton(
    type: DemoPaymentButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (type) {
        DemoPaymentButtonType.PAYPAL -> AndroidView(
            factory = { context ->
                PayPalButton(context).apply { setOnClickListener { onClick() } }
            },
            update = { button ->
                button.color = PayPalButtonColor.BLUE
                button.label = PayPalButtonLabel.PAY
                button.size = PaymentButtonSize.LARGE
            },
            modifier = modifier
        )

        DemoPaymentButtonType.CARD -> AndroidView(
            factory = { context ->
                CardButton(context).apply { setOnClickListener { onClick() } }
            },
            update = { button ->
                button.label = CardButtonLabel.PAY
            },
            modifier = modifier
        )
    }
}

@Preview
@Composable
fun StatefulActionPaymentButtonPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                ActionPaymentButtonColumn(
                    type = DemoPaymentButtonType.CARD,
                    state = ActionState.Idle,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UIConstants.paddingMedium)
                ) { state ->
                    Text(text = "Sample Text", modifier = Modifier.padding(64.dp))
                }
            }
        }
    }
}
