package com.paypal.android.ui.paypal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton

@Composable
fun StartPayPalNativeCheckoutForm(
    shippingPreference: ShippingPreferenceType,
    isLoading: Boolean,
    onShippingPreferenceSelected: (ShippingPreferenceType) -> Unit,
    onSubmit: () -> Unit
) {

    val getFromFileValue = stringResource(R.string.shipping_preference_get_from_file)
    val noShippingValue = stringResource(R.string.shipping_preference_no_shipping)
    val setProvidedAddressValue = stringResource(R.string.shipping_preference_set_provided_address)

    val selectedValue = when (shippingPreference) {
        ShippingPreferenceType.GET_FROM_FILE -> getFromFileValue
        ShippingPreferenceType.NO_SHIPPING -> noShippingValue
        ShippingPreferenceType.SET_PROVIDED_ADDRESS -> setProvidedAddressValue
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Launch PayPal Web Checkout",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(16.dp))
            OptionList(
                title = stringResource(id = R.string.shipping_preference),
                options = listOf(getFromFileValue, noShippingValue, setProvidedAddressValue),
                selectedOption = selectedValue,
                onOptionSelected = { option ->
                    val newShippingPreferenceValue = when (option) {
                        getFromFileValue -> ShippingPreferenceType.GET_FROM_FILE
                        noShippingValue -> ShippingPreferenceType.NO_SHIPPING
                        setProvidedAddressValue -> ShippingPreferenceType.SET_PROVIDED_ADDRESS
                        else -> null
                    }
                    newShippingPreferenceValue?.let { onShippingPreferenceSelected(it) }
                }
            )
            Spacer(modifier = Modifier.size(16.dp))
            WireframeButton(
                text = "Start Checkout",
                isLoading = isLoading,
                onClick = { onSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}