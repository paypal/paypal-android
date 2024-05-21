package com.paypal.android.ui.paypalstaticbuttons

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.paypal.android.R

@Composable
fun PayPalStaticButtonsView() {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.pay_later_button_test_layout, null, false)

            view
        }
    )
}
