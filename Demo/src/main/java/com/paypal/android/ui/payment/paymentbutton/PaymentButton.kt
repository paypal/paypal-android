package com.paypal.android.ui.payment.paymentbutton

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.paypal.android.R

@RequiresApi(Build.VERSION_CODES.M)
abstract class PaymentButton<C : PaymentButtonColor> @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {

    /**
     * Signals that the backing shape has changed and may require a full redraw.
     */
    private var shapeHasChanged = false

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
    abstract var color: C

    /**
     * Provides the current [PaymentButtonEligibilityStatus]. This value can change after the button
     * has been rendered, those changes can be observed on by using either
     * [onEligibilityStatusChanged] or [paymentButtonEligibilityStatusChanged].
     */
    private var eligibilityStatus: PaymentButtonEligibilityStatus = PaymentButtonEligibilityStatus.Loading
        private set(value) {
            field = value
            renderButtonForEligibility()
            onEligibilityStatusChanged?.invoke(field)
            paymentButtonEligibilityStatusChanged?.onPaymentButtonEligibilityStatusChanged(field)
        }

    /**
     * Used for observing on [eligibilityStatus] changes. As soon as this property is set, the
     * provided function will be invoked with the current value of [eligibilityStatus]. This is the
     * preferred way of observing on status changes for Kotlin.
     */
    var onEligibilityStatusChanged: ((buttonEligibilityStatus: PaymentButtonEligibilityStatus) -> Unit)? = null
        set(value) {
            field = value
            field?.invoke(eligibilityStatus)
        }

    /**
     * Used for observing on [eligibilityStatus] changes. As soon as this property is set, the
     * provided interface will be invoked with the current value of [eligibilityStatus]. This is the
     * preferred way of observing on status changes for Java.
     */
    var paymentButtonEligibilityStatusChanged: PaymentButtonEligibilityStatusChanged? = null
        set(value) {
            field = value
            field?.onPaymentButtonEligibilityStatusChanged(eligibilityStatus)
        }

    /**
     * Provides the current value of the prefix text which is displayed before the button's wordmark.
     */
    var prefixText: String? = null
        protected set(value) {
            field = value
            prefixTextView.text = field
        }

    /**
     * Provides the current visibility of the prefix text which is displayed before the button's
     * wordmark.
     */
    var prefixTextVisibility: Int = View.GONE
        protected set(value) {
            field = value
            updatePrefixTextVisibility()
        }

    /**
     * Updates the shape of the Payment Button with the provided [PaymentButtonShape]
     * and defaults to [ROUNDED] if one is not provided.
     *
     * If your application is taking advantage of Material Theming then your own shape definitions
     * will be used as the default.
     */
    var shape: PaymentButtonShape = PaymentButtonShape.ROUNDED
        set(value) {
            shapeHasChanged = field != value
            field = value

            val cornerRadius = when (field) {
                PaymentButtonShape.ROUNDED -> {
                    resources.getDimension(R.dimen.paypal_payment_button_corner_radius_rounded)
                }
                PaymentButtonShape.PILL -> height.toFloat()
                PaymentButtonShape.RECTANGLE -> {
                    resources.getDimension(R.dimen.paypal_payment_button_corner_radius_square)
                }
            }

            val cornerTreatment = when (field) {
                PaymentButtonShape.ROUNDED, PaymentButtonShape.PILL -> RoundedCornerTreatment()
                PaymentButtonShape.RECTANGLE -> CutCornerTreatment()
            }

            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(cornerRadius)
                .setAllCorners(cornerTreatment)
                .build()
        }

    /**
     * Provides the current value of the suffix text which is displayed after the button's wordmark.
     */
    var suffixText: String? = null
        protected set(value) {
            field = value
            suffixTextView.text = field
        }

    /**
     * Provides the current visibility of the suffix text which is displayed before the button's
     * wordmark.
     */
    var suffixTextVisibility: Int = View.GONE
        protected set(value) {
            field = value
            updateSuffixTextVisibility()
        }

    protected abstract val wordmarkDarkLuminanceResId: Int

    protected abstract val wordmarkLightLuminanceResId: Int

    internal abstract val fundingType: PaymentButtonFundingType

    private var payPalWordmarkImage: ImageView
    private var progressBar: ProgressBar
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
            .inflate(R.layout.paypal_payment_button_view, this, true)

        prefixTextView = findViewById(R.id.prefixText)
        suffixTextView = findViewById(R.id.suffixText)
        payPalWordmarkImage = findViewById(R.id.payPalWordmarkImage)
        progressBar = findViewById(R.id.progressBar)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        eligibilityStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PaymentButtonEligibilityStatus.Loading
        } else {
            PaymentButtonEligibilityStatus.Ineligible
        }

        initAttributes(attributeSet, defStyleAttr)
    }

    private fun updateEligibility() {
        updateEligibilityStatus(true)
    }

    /**
     * [fundingEligibilityResponse]: Should get a response from the PayPal SDK whether you could use
     * the listed Payment Methods
     *
     */
    private fun updateEligibilityStatus(fundingEligibilityResponse: Boolean) {
        fundingEligibilityResponse?.let { _ ->
            val isEligible = when (fundingType) {
                PaymentButtonFundingType.PAYPAL -> {
                    fundingEligibilityResponse
                }
                PaymentButtonFundingType.PAYPAL_CREDIT -> {
                    fundingEligibilityResponse
                }
            }
            eligibilityStatus = if (isEligible) {
                PaymentButtonEligibilityStatus.Eligible
            } else {
                PaymentButtonEligibilityStatus.Ineligible
            }
        }
    }

    private fun noEligibilityFound() {
        eligibilityStatus = PaymentButtonEligibilityStatus.Error
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateEligibility()
    }

    override fun onDraw(canvas: Canvas?) {
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

    /**
     * setVisibility will first check to see if the button is eligible, if it's not then the button
     * will always be set to [View.GONE]. If the button is eligible then it will use the value of
     * [visibility] that was provided.
     */
    override fun setVisibility(visibility: Int) {
        val resolvedVisibility = when (eligibilityStatus) {
            is PaymentButtonEligibilityStatus.Eligible, PaymentButtonEligibilityStatus.Loading -> {
                visibility
            }
            else -> View.GONE
        }

        super.setVisibility(resolvedVisibility)
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

    protected fun updateShapeDrawableFillColor(updatedColor: C) {
        if (eligibilityStatus != PaymentButtonEligibilityStatus.Eligible) return

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

    private fun updatePrefixTextVisibility() {
        when (eligibilityStatus) {
            is PaymentButtonEligibilityStatus.Loading -> prefixTextView.visibility = View.GONE
            else ->
                prefixTextView.visibility = prefixTextVisibility
        }
    }

    private fun updateSuffixTextVisibility() {
        when (eligibilityStatus) {
            is PaymentButtonEligibilityStatus.Loading -> suffixTextView.visibility = View.GONE
            else ->
                suffixTextView.visibility = suffixTextVisibility
        }
    }

    private fun renderButtonForEligibility() {
        when (eligibilityStatus) {
            PaymentButtonEligibilityStatus.Eligible -> {
                progressBar.visibility = GONE
                payPalWordmarkImage.visibility = VISIBLE
                updateShapeDrawableFillColor(color)
                updateSuffixTextVisibility()
                updatePrefixTextVisibility()
                isEnabled = true
                visibility = VISIBLE
            }
            PaymentButtonEligibilityStatus.Ineligible, PaymentButtonEligibilityStatus.Error -> {
                isEnabled = false
                visibility = GONE
            }
            PaymentButtonEligibilityStatus.Loading -> {
                isEnabled = false
                payPalWordmarkImage.visibility = GONE
                progressBar.visibility = VISIBLE
                materialShapeDrawable = materialShapeDrawable.apply {
                    fillColor = ContextCompat.getColorStateList(context, R.color.paypal_silver)
                }
                visibility = VISIBLE
            }
        }.exhaustive
    }
}

/**
 * Provides the current eligibility status of a [PaymentButton]. Instances of [PaymentButton] are
 * reliant on funding eligibility requirements, while we are retrieving eligibility information the
 * button will be in the [Loading] status, and will be visible with a progress bar. If a button is
 * not eligible for display then it will have a status of [Ineligible] and the button's visibility
 * will be set to [View.GONE]. When a [PaymentButton] is [Eligible] then it's visibility will remain
 * unchanged (if it was [View.VISIBLE] it will remain in that state).
 */
sealed class PaymentButtonEligibilityStatus {
    /**
     * [Eligible] means that the [PaymentButton] can be rendered and interacted with.
     */
    object Eligible : PaymentButtonEligibilityStatus()

    /**
     * [Ineligible] means that the [PaymentButton] cannot be rendered and buyer's won't be able to
     * interact with it.
     */
    object Ineligible : PaymentButtonEligibilityStatus()

    /**
     * [Loading] means that the [PaymentButton] is currently retrieving the eligibility status and
     * will appear with a loading indicator if the button is set to visible.
     */
    object Loading : PaymentButtonEligibilityStatus()

    /**
     * [Error] means that the [PaymentButton] was unable to determine its eligibility status after
     * 30 seconds. This generally happens if there was an error with the funding eligibility network
     * request and is not expected to occur at any meaningful interval. In general a button is able
     * to determine its funding eligibility with 250ms, providing the SDK has been initialized at
     * app startup then eligibility information should be available before the button even renders.
     */
    object Error : PaymentButtonEligibilityStatus()

    override fun toString(): String = this::class.java.simpleName
}

internal enum class PaymentButtonFundingType {
    PAYPAL,
    PAYPAL_CREDIT;
}

/**
 * Provides a way to observe on eligibility status changes.
 */
interface PaymentButtonEligibilityStatusChanged {

    /**
     * Invoked whenever the [PaymentButtonEligibilityStatus] changes for a given [PaymentButton].
     */
    fun onPaymentButtonEligibilityStatusChanged(
        paymentButtonEligibilityStatus: PaymentButtonEligibilityStatus
    )
}

/**
 * Should be used if a `when` statement must be exhaustive but the expression is unused. Using this
 * property will result in a compile error if the `when` statement is not exhaustive.
 */
internal val Any?.exhaustive: Unit
    get() = Unit
