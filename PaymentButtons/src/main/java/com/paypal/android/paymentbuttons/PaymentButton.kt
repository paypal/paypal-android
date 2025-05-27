package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.ui.R
import kotlin.math.round


@Suppress("TooManyFunctions")
abstract class PaymentButton<C : PaymentButtonColor> @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {

    companion object {
        private const val LOGO_TO_BUTTON_HEIGHT_RATIO = 0.58f
        private const val TEXT_TO_LOGO_HEIGHT_RATIO = 0.58f
    }

    internal val analyticsService: AnalyticsService =
        AnalyticsService(context, CoreConfig(clientId = "N/A", environment = Environment.LIVE))

    private var shapeAppearanceModel: ShapeAppearanceModel = ShapeAppearanceModel()
        set(value) {
            field = value
            materialShapeDrawable = materialShapeDrawable.apply {
                shapeAppearanceModel = field
            }
        }

    private var materialShapeDrawable: MaterialShapeDrawable = MaterialShapeDrawable()
        set(value) {
            field = value
            background = field
        }

    private val colorLuminance: PaymentButtonColorLuminance
        get() = color.luminance

    /**
     * Updates the color of the Payment Button with the provided [PaymentButtonColor].
     */
    abstract var color: PaymentButtonColor

    /**
     * The prefix text which is displayed before the button's wordmark.
     */
    var prefixText: String? = null
        protected set(value) {
            field = value
            prefixTextView.text = field
        }

    /**
     * Visibility of the prefix text which is displayed before the button's
     * wordmark.
     */
    var prefixTextVisibility: Int = View.GONE
        protected set(value) {
            field = value
            prefixTextView.visibility = prefixTextVisibility
        }

    var edges: PaymentButtonEdges = PaymentButtonEdges.Soft
        set(value) {
            field = value
            applyEdgeStyling()
        }

    /**
     * Value of the suffix text which is displayed after the button's wordmark.
     */
    var suffixText: String? = null
        protected set(value) {
            field = value
            suffixTextView.text = field
        }

    /**
     * Visibility of the suffix text which is displayed before the button's
     * wordmark.
     */
    var suffixTextVisibility: Int = View.GONE
        protected set(value) {
            field = value
            suffixTextView.visibility = suffixTextVisibility
        }

    protected abstract val wordmarkDarkLuminanceResId: Int

    protected abstract val wordmarkLightLuminanceResId: Int

    internal abstract val fundingType: PaymentButtonFundingType

    private var payPalWordmarkImage: ImageView
    private var prefixTextView: TextView
    private var suffixTextView: TextView

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.paypal_ui_payment_button_view, this, true)

        prefixTextView = findViewById(R.id.prefixText)
        suffixTextView = findViewById(R.id.suffixText)
        payPalWordmarkImage = findViewById(R.id.payPalWordmarkImage)

        orientation = VERTICAL

        initAttributes(attributeSet, defStyleAttr)
        applyDefaultAttributes()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        renderButton()
    }

    private fun renderButton() {
        payPalWordmarkImage.visibility = VISIBLE
        updateShapeDrawableFillColor(color)
        suffixTextView.visibility = suffixTextVisibility
        prefixTextView.visibility = prefixTextVisibility
        isEnabled = true
        visibility = VISIBLE
    }

    private fun initAttributes(attributeSet: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(attributeSet, R.styleable.PaymentButton).use { typedArray ->
            updateShapeFrom(typedArray, attributeSet, defStyleAttr)
        }
    }

    private fun applyDefaultAttributes() {
        minimumHeight = resources.getDimension(R.dimen.paypal_payment_button_min_height).toInt()

        // set explicit height if none given; for percentage layout to work, this button needs
        // an explicitly set height
        val needsExplicitHeight =
            (layoutParams == null || layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT)
        if (needsExplicitHeight) {
            val width = layoutParams?.width ?: ViewGroup.LayoutParams.MATCH_PARENT
            val height =
                resources.getDimensionPixelSize(R.dimen.paypal_payment_button_default_height)
            layoutParams = ViewGroup.LayoutParams(width, height)
        }

        val textSize = calculateTextSizeInPixelsRelativeToLayoutHeight()
        prefixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        suffixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    private fun calculateTextSizeInPixelsRelativeToLayoutHeight(): Float {
        val textSizeAdjustment =
            resources.getDimensionPixelSize(R.dimen.paypal_payment_button_text_size_adjustment)
        val proportionalTextSize =
            round(layoutParams.height * LOGO_TO_BUTTON_HEIGHT_RATIO * TEXT_TO_LOGO_HEIGHT_RATIO)
        return proportionalTextSize + textSizeAdjustment
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener { view ->
            listener?.onClick(view)
            analyticsService.sendAnalyticsEvent(
                "payment-button:tapped",
                orderId = null,
                buttonType = fundingType.buttonType
            )
        }
    }

    private fun updateShapeFrom(
        typedArray: TypedArray,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) {
        val shapeAttributeExists =
            typedArray.hasValue(R.styleable.PaymentButton_payment_button_edges)
        if (shapeAttributeExists) {
            val edgesAttribute = typedArray.getInt(
                R.styleable.PaymentButton_payment_button_edges,
                PaymentButtonEdges.PAYMENT_BUTTON_EDGE_INT_VALUE_DEFAULT
            )
            PaymentButtonEdges.fromInt(edgesAttribute)?.let { edges = it }
        } else {
            useThemeShapeAppearance(attributeSet, defStyleAttr)
        }
    }

    private fun useThemeShapeAppearance(attributeSet: AttributeSet?, defStyleAttr: Int) {
        shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, attributeSet, defStyleAttr, R.style.Widget_MaterialComponents_Button)
            .build()
    }

    private fun updateButtonStroke() {
        materialShapeDrawable = if (color.hasOutline) {
            val strokeColor = ContextCompat.getColor(context, R.color.neutral_white_border)
            val strokeWidth = resources.getDimension(R.dimen.paypal_payment_button_stroke_width)
            materialShapeDrawable.apply { setStroke(strokeWidth, strokeColor) }
        } else {
            val strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
            val strokeWidth = 0f
            materialShapeDrawable.apply { setStroke(strokeWidth, strokeColor) }
        }
    }

    protected fun updateShapeDrawableFillColor(updatedColor: PaymentButtonColor) {
        materialShapeDrawable = materialShapeDrawable.apply {
            fillColor = updatedColor.retrieveColorResource(context)

            updateButtonStroke()
        }
        updateButtonWordmark()
        updateButtonTextColor()
    }

    private fun updateButtonWordmark() {
        val wordmark = when (colorLuminance) {
            PaymentButtonColorLuminance.LIGHT -> {
                ContextCompat.getDrawable(context, wordmarkLightLuminanceResId)
            }

            PaymentButtonColorLuminance.DARK -> {
                ContextCompat.getDrawable(context, wordmarkDarkLuminanceResId)
            }
        }
        payPalWordmarkImage.setImageDrawable(wordmark)
    }

    private fun updateButtonTextColor() {
        val textColor = when (colorLuminance) {
            PaymentButtonColorLuminance.LIGHT -> {
                ContextCompat.getColor(context, R.color.paypal_spb_on_light_surface)
            }

            PaymentButtonColorLuminance.DARK -> {
                ContextCompat.getColor(context, R.color.paypal_spb_on_dark_surface)
            }
        }
        prefixTextView.setTextColor(textColor)
        suffixTextView.setTextColor(textColor)
    }

    private fun applyEdgeStyling() {
        val cornerSize: Float = when (val edges = edges) {
            PaymentButtonEdges.Sharp -> 0f
            PaymentButtonEdges.Pill -> layoutParams.height / 2f
            PaymentButtonEdges.Soft -> getSoftCornerRadiusDimensionValue()
            is PaymentButtonEdges.Custom -> edges.cornerRadius
        }

        val cornerTreatment =
            if (cornerSize == 0.0f) CutCornerTreatment() else RoundedCornerTreatment()
        shapeAppearanceModel = ShapeAppearanceModel.builder().apply {
            setAllCorners(cornerTreatment)
            setAllCornerSizes(cornerSize)
        }.build()
    }

    private fun getSoftCornerRadiusDimensionValue(): Float {
        @DimenRes val cornerSizeResId = R.dimen.paypal_payment_button_corner_radius_soft
        return resources.getDimensionPixelSize(cornerSizeResId).toFloat()
    }
}

internal enum class PaymentButtonFundingType(val buttonType: String) {
    PAYPAL("PayPal"),
    PAY_LATER("Pay Later"),
    PAYPAL_CREDIT("Credit");
}
