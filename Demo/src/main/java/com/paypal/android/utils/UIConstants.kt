package com.paypal.android.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

object UIConstants {
    private const val slideInOffsetPercentY = 0.05
    fun getSlideInStartOffsetY(fullHeightOfComposable: Int) =
        (fullHeightOfComposable * slideInOffsetPercentY).toInt()

    // Ref: https://support.google.com/accessibility/android/answer/7101858
    val minimumTouchSize = 48.dp
    val stepNumberBackgroundSize = 40.dp
    val buttonCornerRadius = 16.dp
    val chevronSize = 16.dp
    val progressIndicatorSize = 32.dp

    val paddingExtraSmall = 4.dp
    val paddingSmall = 8.dp
    val paddingMedium = 16.dp
    val paddingLarge = 24.dp

    val spacingExtraSmall = Arrangement.spacedBy(paddingExtraSmall)
    val spacingSmall = Arrangement.spacedBy(paddingSmall)
    val spacingMedium = Arrangement.spacedBy(paddingMedium)
    val spacingLarge = Arrangement.spacedBy(paddingLarge)
}
