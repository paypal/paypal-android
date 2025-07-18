package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor.BLACK
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor.BLUE
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor.WHITE
import com.paypal.android.paymentbuttons.error.createFormattedIllegalArgumentException
import com.paypal.android.ui.R

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
    override var color: PaymentButtonColor = BLUE
        set(value) {
            field = value
            updateShapeDrawableFillColor(field)
        }

    override val wordmarkDarkLuminanceResId: Int = R.drawable.paypal_logo_white
    override val wordmarkLightLuminanceResId: Int = R.drawable.paypal_logo_black

    override val fundingType: PaymentButtonFundingType = PaymentButtonFundingType.PAYPAL_CREDIT

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.PayPalCreditButton)
            .use { typedArray ->
                updateColorFrom(typedArray)
            }
        contentDescription = context.getString(R.string.paypal_payment_credit_button_description)
        configurePayPalCreditLayout()
        analyticsService.sendAnalyticsEvent(
            "payment-button:initialized",
            orderId = null,
            buttonType = PaymentButtonFundingType.PAYPAL_CREDIT.buttonType
        )
    }

    private fun updateColorFrom(typedArray: TypedArray) {
        val attribute = typedArray.getInt(
            R.styleable.PayPalCreditButton_paypal_credit_color,
            BLUE.value
        )
        color = PayPalCreditButtonColor(attribute)
    }

    private fun configurePayPalCreditLayout() {
        prefixTextVisibility = View.GONE
        suffixTextVisibility = View.VISIBLE

        val stringResId = R.string.paypal_checkout_smart_payment_button_label_credit
        suffixText = context.getString(stringResId)
    }
}

/**
 * Defines the colors available for PayPal Credit buttons.
 *
 * @see BLUE is the default and recommended color for PayPal Credit
 * @see WHITE alternative color for PayPal Credit
 * @see BLACK is a secondary alternative color for PayPal Credit
 */
enum class PayPalCreditButtonColor(
    val value: Int,
    override val colorResId: Int,
    override val hasOutline: Boolean = false,
    override val luminance: PaymentButtonColorLuminance
) : PaymentButtonColor {
    BLUE(
        value = 0,
        colorResId = R.color.paypal_blue,
        luminance = PaymentButtonColorLuminance.LIGHT
    ),
    WHITE(
        value = 1,
        colorResId = R.color.paypal_white,
        hasOutline = true,
        luminance = PaymentButtonColorLuminance.LIGHT
    ),
    BLACK(
        value = 2,
        colorResId = R.color.paypal_black,
        luminance = PaymentButtonColorLuminance.DARK
    );

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PayPalCreditButtonColor].
         * If an invalid [attributeIndex] is provided then it will throw an [IllegalArgumentException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PayPalCreditButtonColor {
            return when (attributeIndex) {
                BLUE.value -> BLUE
                BLACK.value -> BLACK
                WHITE.value -> WHITE
                else -> throw createFormattedIllegalArgumentException(
                    "PayPalCreditButtonColor",
                    entries.size
                )
            }
        }
    }
}
