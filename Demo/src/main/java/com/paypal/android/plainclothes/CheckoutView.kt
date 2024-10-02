package com.paypal.android.plainclothes

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.paymentbuttons.PaymentButtonSize
import com.paypal.android.utils.UIConstants

@Composable
fun CheckoutView() {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Plain T-Shirt",
            style = MaterialTheme.typography.displayMedium
        )
        Image(
            painter = painterResource(id = R.drawable.plain_tshirt),
            contentDescription = stringResource(id = R.string.plain_tshirt_description),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(2.dp, Color.Black)
                .padding(UIConstants.paddingExtraSmall)
        )
        Row {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "29.99",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .weight(1f)
            )
        }
        PayPalButton(
            onClick = {

            }
        )
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            onClick = { /*TODO*/ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Pay with Card",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun PayPalButton(onClick: () -> Unit) {
    AndroidView(
        factory = { context ->
            PayPalButton(context).apply {
                setOnClickListener {
                    onClick()
                }
            }
        },
        update = { button ->
            button.shape = PaymentButtonShape.ROUNDED
            button.size = PaymentButtonSize.MEDIUM
            button.label = PayPalButtonLabel.PAY
        },
        modifier = Modifier.fillMaxWidth()
    )

}

@Preview
@Composable
fun CheckoutPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutView()
        }
    }
}
