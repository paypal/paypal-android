package com.paypal.android.paymentbuttons

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.Button
import android.widget.RelativeLayout
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
) : RelativeLayout(context, attributeSet, defStyleAttr) {

    init {
        inflate(context, R.layout.paypal_button_v2, this)
//        // Ref: https://stackoverflow.com/a/3177667
//        val textPrefix = "Pay with ^"
//        val spannableString = SpannableString(textPrefix)
//        val alignmentSpan = AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
//        spannableString.setSpan(alignmentSpan, 0, "Pay with".length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
//
//        val wordmarkDrawable = ContextCompat.getDrawable(context, R.drawable.wordmark_paypal_v2)!!
//        wordmarkDrawable.setBounds(
//            0,
//            0,
//            wordmarkDrawable.intrinsicWidth,
//            wordmarkDrawable.intrinsicHeight
//        )
//        val imageSpan = ImageSpan(wordmarkDrawable, ImageSpan.ALIGN_BASELINE)
//        spannableString.setSpan(
//            imageSpan,
//            textPrefix.length - 1,
//            textPrefix.length,
//            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
//        )
//        text = "Pay with"
//
////        minHeight = context.toPx(48).toInt()
//        gravity = Gravity.CENTER
//
//        // TODO: leverage material shape drawable to programmatically set corner radius and maintain ripple effect
//        val drawable = ContextCompat.getDrawable(context, R.drawable.paypal_button_v2_background)
//        background = drawable
//        setCompoundDrawables(null, null, wordmarkDrawable, null)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        findViewById<Button>(R.id.button).setOnClickListener(l)
    }
}