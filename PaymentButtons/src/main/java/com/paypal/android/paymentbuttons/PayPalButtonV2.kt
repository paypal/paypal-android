package com.paypal.android.paymentbuttons

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.paypal.android.ui.R

// Ref: https://stackoverflow.com/a/6327095
fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    resources.displayMetrics
)

class PayPalButtonV2 @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attributeSet, defStyleAttr) {

    init {
        text = "helloooo"
        minHeight = context.toPx(48).toInt()
        gravity = Gravity.CENTER

        // TODO: leverage material shape drawable to programmatically set corner radius and maintain ripple effect
        val drawable = ContextCompat.getDrawable(context, R.drawable.paypal_button_v2_background)
        background = drawable
    }
}