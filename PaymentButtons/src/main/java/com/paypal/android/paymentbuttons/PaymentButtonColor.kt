package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

/**
 * PaymentButtonColor provides a structure for all colors that can be used within a [PaymentButton].
 *
 * @property colorResId is the color resource ID that can be used for displaying the color.
 * @property hasOutline when true the color should be displayed with an outline, when false then
 * no outline will be drawn.
 * @property luminance defines the luminance of the color, useful when determining which type of
 * wordmark to use or when determining the surface color of text widgets on the button.
 */
interface PaymentButtonColor {
    val colorResId: Int

    val hasOutline: Boolean

    val luminance: PaymentButtonColorLuminance

    /**
     * Provides the correct [ColorStateList] given a [Context].
     *
     * @return [ColorStateList] which corresponds to the [PayPalButtonColor].
     */
    fun retrieveColorResource(context: Context): ColorStateList {
        return ContextCompat.getColorStateList(context, colorResId)!!
    }
}

/**
 * ColorLuminance defines the intensity of the light emitted by a given color with simplified
 * [DARK] and [LIGHT] values.
 *
 * This is helpful when modifying internal button styles (wordmark and text for example) to
 * change those colors based on the button's background color while still allow for exhaustive
 * compile time checks (something not currently possible with booleans).
 */
enum class PaymentButtonColorLuminance {
    DARK, LIGHT;
}
