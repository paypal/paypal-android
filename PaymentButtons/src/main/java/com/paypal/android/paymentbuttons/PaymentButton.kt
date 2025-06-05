package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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
import kotlin.math.max
import kotlin.math.min
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

        private fun clamp(value: Int, min: Int, max: Int): Int {
            return min(max(value, min), max)
        }
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

    private val defaultButtonHeight: Int
    private val minButtonHeight: Int
    private val maxButtonHeight: Int

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.paypal_ui_payment_button_view, this, true)

        prefixTextView = findViewById(R.id.prefixText)
        suffixTextView = findViewById(R.id.suffixText)
        payPalWordmarkImage = findViewById(R.id.payPalWordmarkImage)

        orientation = VERTICAL
        gravity = Gravity.CENTER

        initAttributes(attributeSet, defStyleAttr)

        // resolve these values at initialization time and cache them to avoid expensive function
        // calls in onMeasure
        defaultButtonHeight =
            resources.getDimensionPixelSize(R.dimen.paypal_payment_button_default_height)
        minButtonHeight = resources.getDimensionPixelSize(R.dimen.paypal_payment_button_min_height)
        maxButtonHeight = resources.getDimensionPixelSize(R.dimen.paypal_payment_button_max_height)

        minimumHeight = minButtonHeight
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        renderButton()
//        constrainLayoutParams()
        addOnLayoutChangeListener({ view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            (view as? PaymentButton<*>)?.updateFontSizing(bottom - top)
        })
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

    private fun constrainLayoutParams() {
        // For PayPal logo and prefix/suffix font sizes to be calculated using
        // relative percentages, this button needs an explicit height.
        val layoutHeight = layoutParams?.height
        val height = if (
            layoutHeight == null
            || layoutHeight == LayoutParams.WRAP_CONTENT
            || layoutHeight == LayoutParams.MATCH_PARENT
        ) {
            // if no height given, use the default height
            resources.getDimensionPixelSize(R.dimen.paypal_payment_button_default_height)
        } else {
            val minHeight =
                resources.getDimensionPixelSize(R.dimen.paypal_payment_button_min_height)
            val maxHeight =
                resources.getDimensionPixelSize(R.dimen.paypal_payment_button_max_height)
            clamp(layoutHeight, minHeight, maxHeight)
        }
        val width = layoutParams?.width ?: LayoutParams.WRAP_CONTENT
        layoutParams = LayoutParams(width, height)


//        updateFontSizing(height)
//        addOnLayoutChangeListener({ view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
//            (view as? PaymentButton<*>)?.updateFontSizing(bottom - top)
//        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ref: https://stackoverflow.com/a/23617530
        // Ref: https://stackoverflow.com/a/10339611
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightOverride = when (hMode) {
            MeasureSpec.AT_MOST -> clamp(hSize, minButtonHeight, defaultButtonHeight)
            MeasureSpec.EXACTLY -> clamp(hSize, minButtonHeight, maxButtonHeight)
            else -> defaultButtonHeight
        }

        val heightMeasureSpecOverride =
            MeasureSpec.makeMeasureSpec(heightOverride, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightMeasureSpecOverride)
    }

    private fun calculateTextSizeInPixelsRelativeToLayoutHeight(height: Int): Float {
        val textSizeAdjustment =
            resources.getDimensionPixelSize(R.dimen.paypal_payment_button_text_size_adjustment)
        val proportionalTextSize =
            round(height * LOGO_TO_BUTTON_HEIGHT_RATIO * TEXT_TO_LOGO_HEIGHT_RATIO)
        return proportionalTextSize + textSizeAdjustment
    }

    private fun updateFontSizing(height: Int) {
        val textSize = calculateTextSizeInPixelsRelativeToLayoutHeight(height)
        prefixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        suffixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
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
