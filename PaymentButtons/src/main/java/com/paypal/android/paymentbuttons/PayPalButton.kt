package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import com.paypal.android.paymentbuttons.error.createFormattedIllegalArgumentException
import com.paypal.android.ui.R

/**
 * PayPalButton provides a PayPal button with the ability to modify the [color], [label], [shape],
 * and [size].
 *
 * Setting up PayPalButton within an XML layout:
 * ```
 * <PayPalButton
 *      android:id="@+id/paymentButton"
 *      android:layout_width="match_parent"
 *      android:layout_height="wrap_content" />
 * ```
 *
 * Optionally you can provide the following attributes: `paypal_color`, `paypal_label`,
 * `payment_button_shape`, and `payment_button_size`.
 *
 */
open class PayPalButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PaymentButton<PayPalButtonColor>(context, attributeSet, defStyleAttr) {

    /**
     * Updates the color of the Payment Button with the provided [PayPalButtonColor].
     *
     * This may update the PayPal wordmark to aid with visibility as well. When updated to GOLD or
     * WHITE it will be updated to the traditional wordmark. When updated to BLUE or BLACK it will
     * be updated to the monochrome wordmark.
     */
    override var color: PaymentButtonColor = PayPalButtonColor.GOLD
        set(value) {
            field = value
            updateShapeDrawableFillColor(field)
        }

    /**
     * Updates the label for Payment Button with the provided [PayPalButtonLabel].
     *
     * This will default to [PAYPAL] if one is not provided which omits a label and only displays
     * the wordmark. Note: this does not support [PAY_LATER], if you require a button with that
     * label then use the specialized [PayLaterButton].
     */
    open var label: PayPalButtonLabel = PayPalButtonLabel.PAYPAL
        set(value) {
            if (value != PayPalButtonLabel.PAY_LATER) {
                field = value
                updateLabel(field)
            }
        }

    override val wordmarkDarkLuminanceResId: Int = R.drawable.wordmark_paypal_monochrome

    override val wordmarkLightLuminanceResId: Int = R.drawable.wordmark_paypal_color

    override val fundingType: PaymentButtonFundingType = PaymentButtonFundingType.PAYPAL

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.PayPalButton).use { typedArray ->
            updateColorFrom(typedArray)
            updateLabelFrom(typedArray)
        }
        contentDescription = context.getString(R.string.paypal_payment_button_description)
    }

    private fun updateColorFrom(typedArray: TypedArray) {
        val paypalColorAttribute = typedArray.getInt(
            R.styleable.PayPalButton_paypal_color,
            PayPalButtonColor.GOLD.value
        )
        color = PayPalButtonColor(paypalColorAttribute)
    }

    private fun updateLabelFrom(typedArray: TypedArray) {
        val paypalLabelAttribute = typedArray.getInt(
            R.styleable.PayPalButton_paypal_label, 0
        )
        label = PayPalButtonLabel(paypalLabelAttribute)
    }

    protected fun updateLabel(updatedLabel: PayPalButtonLabel) {
        when (updatedLabel.position) {
            PayPalButtonLabel.Position.START -> {
                suffixTextVisibility = View.GONE
                prefixTextVisibility = View.VISIBLE
                prefixText = updatedLabel.retrieveLabel(context)
            }
            PayPalButtonLabel.Position.END -> {
                prefixTextVisibility = View.GONE
                suffixTextVisibility = View.VISIBLE
                suffixText = updatedLabel.retrieveLabel(context)
            }
            else -> {
                prefixTextVisibility = View.GONE
                suffixTextVisibility = View.GONE
            }
        }
    }
}

/**
 * Defines the colors available for PayPal buttons.
 *
 * @see GOLD is the default color if one is not provided and is the recommended choice as research
 * has shown it results in the best conversion.
 * @see WHITE is the preferred alternative color if gold does not work for your experience.
 * @see BLUE is one of our secondary alternatives. This color is less capable of drawing people's
 * attention. Deprecated - please use GOLD or WHITE.
 * @see BLACK is one of our secondary alternatives. This color is less capable of drawing people's
 * attention. Deprecated - please use GOLD or WHITE.
 * @see SILVER is one of our secondary alternatives. This color is less capable of drawing people's
 * attention. Deprecated - please use GOLD or WHITE.
 */
enum class PayPalButtonColor(
    val value: Int,
    override val colorResId: Int,
    override val hasOutline: Boolean = false,
    override val luminance: PaymentButtonColorLuminance
) : PaymentButtonColor {
    GOLD(value = 0, colorResId = R.color.paypal_gold, luminance = PaymentButtonColorLuminance.LIGHT),
    @Deprecated("Deprecated color. Replace with gold or white.")
    BLUE(value = 1, colorResId = R.color.paypal_blue, luminance = PaymentButtonColorLuminance.DARK),
    WHITE(
        value = 2,
        colorResId = R.color.paypal_white,
        hasOutline = true,
        luminance = PaymentButtonColorLuminance.LIGHT
    ),
    @Deprecated("Deprecated color. Replace with gold or white.")
    BLACK(value = 3, colorResId = R.color.paypal_black, luminance = PaymentButtonColorLuminance.DARK),
    @Deprecated("Deprecated color. Replace with gold or white.")
    SILVER(value = 4, colorResId = R.color.paypal_silver, luminance = PaymentButtonColorLuminance.LIGHT);

    companion object {
        /**
         * Given an [attributeIndex] this will provide the correct [PayPalButtonColor]. If an
         * invalid [attributeIndex] is provided then it will throw an [IllegalStateException].
         *
         * @throws [IllegalArgumentException] when an invalid index is provided.
         */
        operator fun invoke(attributeIndex: Int): PayPalButtonColor {
            return when (attributeIndex) {
                GOLD.value -> GOLD
                BLUE.value -> BLUE
                WHITE.value -> WHITE
                BLACK.value -> BLACK
                SILVER.value -> SILVER
                else -> throw createFormattedIllegalArgumentException("PayPalButtonColor", values().size)
            }
        }
    }
}

