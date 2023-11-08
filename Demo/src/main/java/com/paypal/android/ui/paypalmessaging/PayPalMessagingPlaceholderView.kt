package com.paypal.android.ui.paypalmessaging

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.paypal.android.R

class PayPalMessagingPlaceholderView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attributeSet, defStyleAttr) {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.pay_pal_messaging_placeholder_view, this, true)
    }
}
