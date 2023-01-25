package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.core.content.res.use
import com.paypal.android.nativepayments.paymentbutton.error.createFormattedIllegalArgumentException
import com.paypal.android.ui.R
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor.BLACK
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor.DARK_BLUE

/**
 * PayPalCreditButton provides a PayPal Credit button with the ability to modify the [color], [shape],
 * and [size].
 *
 * Setting up PayPalCreditButton within an XML layout:
 * ```
 * <PayPalCreditButton
 *      android:id="@+id/paymentButton"
 *      android:layout_width="match_parent"
 *      android:layout_height="wrap_content" />
 * ```
 *
 * Optionally you can provide the following attributes: `paypal_credit_color`,
 * `payment_button_shape`, and `payment_button_size`.
 *
 */
class PayPalCreditButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PaymentButton<PayPalCreditButtonColor>(context, attributeSet, defStyleAttr) {

    /**
     * Updates the color of the Payment Button with the provided [PayPalCreditButtonColor].
     */
    override var color: PaymentButtonColor = DARK_BLUE
        set(value) {
            field = value
            updateShapeDrawableFillColor(field)
        }

    override val wordmarkDarkLuminanceResId: Int = R.drawable.wordmark_paypal_credit_monochrome

    override val wordmarkLightLuminanceResId: Int
        get() = throw UnsupportedOperationException(LUMINANCE_ERROR)

    override val fundingType: PaymentButtonFundingType = PaymentButtonFundingType.PAYPAL_CREDIT

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.PayPalCreditButton)
            .use { typedArray ->
                updateColorFrom(typedArray)
            }
        contentDescription = context.getString(R.string.paypal_payment_credit_button_description)
    }

    private fun updateColorFrom(typedArray: TypedArray) {
        val attribute = typedArray.getInt(
            R.styleable.PayPalCreditButton_paypal_credit_color,
            DARK_BLUE.value
        )
        color = PayPalCreditButtonColor(attribute)
    }

    companion object {
        const val LUMINANCE_ERROR = "PayPalCreditButton does not have a light luminance compatible wordmark."
    }
}

/**
 * Defines the colors available for PayPal Credit buttons.
 *
 * @see DARK_BLUE is the default and recommended color for PayPal Credit.
 * @see BLACK is a secondary alternative color for PayPal Credit.
 */
enum class PayPalCreditButtonColor(
    val value: Int,
    override val colorResId: Int,
    override val hasOutline: Boolean = false,
    override val luminance: PaymentButtonColorLuminance
) : PaymentButtonColor {
    DARK_BLUE(value = 0, colorResId = R.color.paypal_dark_blue, luminance = PaymentButtonColorLuminance.DARK),
    BLACK(value = 1, colorResId = R.color.paypal_black, luminance = PaymentButtonColorLuminance.DARK);

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PayPalCreditButtonColor].
         * If an invalid [attributeIndex] is provided then it will throw an [IllegalArgumentException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PayPalCreditButtonColor {
            return when (attributeIndex) {
                DARK_BLUE.value -> DARK_BLUE
                BLACK.value -> BLACK
                else -> throw createFormattedIllegalArgumentException("PaymentButtonSize", values().size)
            }
        }
    }
}
