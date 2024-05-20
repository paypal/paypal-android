package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import com.paypal.android.ui.R


/**
 * PayLaterButton provides a PayPal PayLater button with the ability to modify the [color], [shape],
 * and [size].
 *
 * Setting up PayLaterButton within an XML layout:
 * ```
 * <PayPalLater
 *      android:id="@+id/payLaterButton"
 *      android:layout_width="match_parent"
 *      android:layout_height="wrap_content" />
 * ```
 *
 * Optionally you can provide the following attributes: `paypal_color`, `paypal_label`,
 * `payment_button_shape`, and `payment_button_size`.
 */
class PayLaterButton @JvmOverloads constructor(
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
     * Provides the label of the [PayLaterButton]. This value will always be
     * [PaymentButtonFundingType.PAY_LATER], attempting
     * to set the value to anything else will result in that value being ignored.
     */
    var label: PayPalButtonLabel = PayPalButtonLabel.PAY_LATER
        set(value) {
            if (value == PayPalButtonLabel.PAY_LATER) {
                field = value
                updateLabel(field)
            }
        }

    override val wordmarkDarkLuminanceResId: Int
        get() = R.drawable.logo_paypal_monochrome

    override val wordmarkLightLuminanceResId: Int
        get() = R.drawable.logo_paypal_color

    override val fundingType: PaymentButtonFundingType = PaymentButtonFundingType.PAY_LATER

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.PayLaterButton).use { typedArray ->
            updateColorFrom(typedArray)
        }
        updateLabel(PayPalButtonLabel.PAY_LATER)
        analyticsService.sendAnalyticsEvent(
            "payment-button:initialized",
            orderId = null,
            buttonType = PaymentButtonFundingType.PAY_LATER.buttonType
        )
    }

    private fun updateColorFrom(typedArray: TypedArray) {
        val paypalColorAttributeIndex = typedArray.getInt(
            R.styleable.PayLaterButton_paylater_color,
            PayPalButtonColor.GOLD.value
        )
        color = PayPalButtonColor(paypalColorAttributeIndex)
    }

    private fun updateLabel(updatedLabel: PayPalButtonLabel) {
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
