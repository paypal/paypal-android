package com.paypal.android.paymentbuttons

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

@Suppress("TooManyFunctions")
abstract class PaymentButton<C : PaymentButtonColor> @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {

    /**
     * Signals that the backing shape has changed and may require a full redraw.
     */
    private var shapeHasChanged = false

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

    /**
     * Updates the corner radius of the button
     *
     * Cannot be used with PaymentButtonShape
     */
    var customCornerRadius: Float? = null
        set(value) {
            field = value

            if (value == null) return

            val cornerTreatment = if (value == 0.0f) {
                CutCornerTreatment()
            } else {
                RoundedCornerTreatment()
            }

            shapeAppearanceModel = ShapeAppearanceModel.builder().apply {
                value?.let {
                    setAllCornerSizes(it)
                }
                setAllCorners(cornerTreatment)
            }.build()
        }

    /**
     * Updates the shape of the Payment Button with the provided [PaymentButtonShape]
     * and defaults to [ROUNDED] if one is not provided.
     *
     * If your application is taking advantage of Material Theming then your own shape definitions
     * will be used as the default.
     *
     * Cannot be used with customCornerRadius
     */
    var shape: PaymentButtonShape = PaymentButtonShape.ROUNDED
        set(value) {
            shapeHasChanged = field != value
            field = value

            this.customCornerRadius = null

            val cornerRadius = when (field) {
                PaymentButtonShape.ROUNDED -> {
                    resources.getDimension(R.dimen.paypal_payment_button_corner_radius_rounded)
                }
                PaymentButtonShape.PILL -> {
                    resources.getDimension(R.dimen.paypal_payment_button_corner_pill)
                }
                PaymentButtonShape.RECTANGLE -> {
                    resources.getDimension(R.dimen.paypal_payment_button_corner_radius_square)
                }
            }

            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(cornerRadius)
                .build()
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

    /**
     * Updates the size of the Payment Button with the provided [PaymentButtonSize].
     *
     * The main UI elements which change when modifying the size include:
     *  - Minimum height of the button.
     *  - Height and width of the wordmark.
     *
     * The default size of a button is [MEDIUM].
     */
    var size: PaymentButtonSize = PaymentButtonSize.MEDIUM
        set(value) {
            field = value
            minimumHeight = resources.getDimension(field.minHeightResId).toInt()
            val verticalPadding = resources.getDimension(field.verticalPaddingResId).toInt()
            setPadding(paddingLeft, verticalPadding, paddingRight, verticalPadding)

            val labelTextSize = resources.getDimension(field.labelTextSizeResId)
            prefixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize)
            suffixTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize)
        }

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.paypal_ui_payment_button_view, this, true)

        prefixTextView = findViewById(R.id.prefixText)
        suffixTextView = findViewById(R.id.suffixText)
        payPalWordmarkImage = findViewById(R.id.payPalWordmarkImage)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        initAttributes(attributeSet, defStyleAttr)
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

    override fun onDraw(canvas: Canvas) {
        if (shape == PaymentButtonShape.PILL && shapeHasChanged) {
            shape = PaymentButtonShape.PILL // force update since PILL is dependent on view height.
        }
        super.onDraw(canvas)
    }

    private fun initAttributes(attributeSet: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(attributeSet, R.styleable.PaymentButton).use { typedArray ->
            updateSizeFrom(typedArray)
            updateShapeFrom(typedArray, attributeSet, defStyleAttr)
        }
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

    private fun updateSizeFrom(typedArray: TypedArray) {
        val paypalSizeAttribute = typedArray.getInt(
            R.styleable.PaymentButton_payment_button_size,
            PaymentButtonSize.MEDIUM.value
        )
        size = PaymentButtonSize(paypalSizeAttribute)
    }

    private fun updateShapeFrom(typedArray: TypedArray, attributeSet: AttributeSet?, defStyleAttr: Int) {
        val shapeAttributeExists = typedArray.hasValue(R.styleable.PaymentButton_payment_button_shape)
        if (shapeAttributeExists) {
            val paypalShapeAttribute = typedArray.getInt(
                R.styleable.PaymentButton_payment_button_shape,
                PaymentButtonShape.ROUNDED.value
            )
            shape = PaymentButtonShape(paypalShapeAttribute)
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
            val strokeColor = ContextCompat.getColor(context, R.color.paypal_spb_on_white_stroke)
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
}

internal enum class PaymentButtonFundingType(val buttonType: String) {
    PAYPAL("PayPal"),
    PAY_LATER("Pay Later"),
    PAYPAL_CREDIT("Credit");
}
