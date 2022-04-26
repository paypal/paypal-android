package com.paypal.android.ui.paymentbutton

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
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
 */
@RequiresApi(Build.VERSION_CODES.M)
class PayLaterButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PayPalButton(context, attributeSet, defStyleAttr) {

    /**
     * Provides the label of the [PayLaterButton]. This value will always be [PAY_LATER], attempting
     * to set the value to anything else will result in that value being ignored.
     */
    override var label: PayPalButtonLabel = PayPalButtonLabel.PAY_LATER
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

    override val fundingType: PaymentButtonFundingType
        get() = PaymentButtonFundingType.PAY_LATER

    init {
        updateLabel(PayPalButtonLabel.PAY_LATER)
    }
}
