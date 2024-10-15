package com.paypal.android.plainclothes

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.cardpayments.Card
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.paymentbuttons.PaymentButtonSize
import com.paypal.android.ui.approveorder.DateString
import com.paypal.android.uishared.components.ActionButton
import com.paypal.android.uishared.components.CardNumberTextField
import com.paypal.android.uishared.components.ExpirationDateTextField
import com.paypal.android.uishared.components.SecurityCodeTextField
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivity

@Composable
@ExperimentalMaterial3Api
fun CheckoutView(
    onCheckoutSuccess: (orderId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.checkoutSuccessOrderId) {
        // notify checkout success
        uiState.checkoutSuccessOrderId?.let { orderId -> onCheckoutSuccess(orderId) }
    }

    val context = LocalContext.current
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
        PayWithCardButton(
            onClick = {
                viewModel.showCardFormModal()
            },
        )
        PayWithPayPalButton(
            onClick = {
                context.getActivity()?.let { viewModel.checkoutWithPayPal(it) }
            }
        )
    }
    if (uiState.isCardFormModalVisible) {
        CardFormModalBottomSheet(
            isLoading = uiState.isLoading,
            onDismissed = { viewModel.hideCardFormModal() },
            onSubmit = { card ->
                context.getActivity()?.let { viewModel.checkoutWithCard(it, card) }
            }
        )
    } else if (uiState.checkoutError != null) {
        ErrorDialog(
            error = uiState.checkoutError as Throwable,
            onDismissRequest = { viewModel.clearCheckoutError() }
        )
    } else if (uiState.isLoading) {
        LoadingDialog()
    }
}

@Composable
fun PayWithCardButton(onClick: () -> Unit) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
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

@Composable
fun PayWithPayPalButton(onClick: () -> Unit) {
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

@Composable
@ExperimentalMaterial3Api
fun CardFormModalBottomSheet(
    isLoading: Boolean,
    onDismissed: () -> Unit,
    onSubmit: (card: Card) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var cardNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(0.8f),
        sheetState = sheetState,
        onDismissRequest = onDismissed,
    ) {
        Column(
            verticalArrangement = UIConstants.spacingSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Enter Card Details",
                style = MaterialTheme.typography.displaySmall
            )
            Column(
                verticalArrangement = UIConstants.spacingSmall
            ) {
                CardNumberTextField(
                    cardNumber = cardNumber,
                    onValueChange = { value -> cardNumber = value },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = UIConstants.spacingMedium) {
                    ExpirationDateTextField(
                        expirationDate = expirationDate,
                        onValueChange = { value -> expirationDate = value },
                        modifier = Modifier.weight(weight = 1.5f)
                    )
                    SecurityCodeTextField(
                        securityCode = securityCode,
                        onValueChange = { value -> securityCode = value },
                        modifier = Modifier.weight(1.0f)
                    )
                }
            }
            ActionButton(
                text = "SUBMIT",
                isLoading = isLoading,
                onClick = {
                    val dateString = DateString(expirationDate)
                    val card = Card(
                        number = cardNumber,
                        expirationMonth = dateString.formattedMonth,
                        expirationYear = dateString.formattedYear,
                        securityCode = securityCode
                    )
                    onSubmit(card)
                }
            )
        }
    }
}

@Preview
@Composable
@ExperimentalMaterial3Api
fun CheckoutPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutView(onCheckoutSuccess = {})
        }
    }
}
