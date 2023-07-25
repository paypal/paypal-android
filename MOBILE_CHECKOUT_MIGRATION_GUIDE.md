# PayPal Mobile Checkout SDK: Migration Guide

This guide outlines how to update your integration from using the soon-to-be-deprecated [PayPal Mobile Checkout SDK](https://developer.paypal.com/limited-release/paypal-mobile-checkout/) to the new PayPal Mobile [Android SDK](https://github.com/paypal/paypal-android/).

## Pre-Requisites
In order to use this migration guide, you must:

1. Have a server-side integration with the [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2/). Please update to Orders v2 if you're on [Payments V1](https://developer.paypal.com/docs/api/payments/v1/) or [NVP/SOAP](https://developer.paypal.com/api/nvp-soap/).
1. Obtain your client ID. Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID in your PayPal Developer Dashboard.
1. Enable your server to create an [Order ID](https://developer.paypal.com/docs/api/orders/v2/).
1. Enable your server to [PATCH](https://developer.paypal.com/docs/api/orders/v2/#orders_patch) an order.
    * _Note:_ This is **only required** if you create your order ID with [`shipping_preference`](https://developer.paypal.com/docs/api/orders/v2/#definition-order_application_context) = `GET_FROM_FILE`. See step 6 in the guides below.

## Client-Side

*Assuming the pre-requisites are met, this migration should take ~1 developer day to complete.*

1. Add the new SDK to your app

    Add the PayPal SDK to your app-level build.gradle file. See the [CHANGELOG](https://github.com/paypal/paypal-android/blob/main/CHANGELOG.md) for the most recent version.

    ```
    dependencies {
         implementation "com.paypal.android:paypal-native-payments:<LATEST-VERSION>"
         implementation "com.paypal.android:payment-buttons:<LATEST-VERSION>"
    }
    ```

2. Update  Configuration

    * Remove `CheckoutConfig` and related `setConfig()` method from your application class.
    * Instantiate a `CoreConfig` with your client ID from the [pre-requisite](#pre-requisites) steps.
        * *Note*: This no longer needs to live in your application class.
    * Construct a `PayPalNativeCheckoutClient`.
        
    ```diff
    class SampleApp : Application() {

        override fun onCreate() {
            super.onCreate()

    -        val config = CheckoutConfig(
    -            application = this,
    -            clientId = "AUiHPkr1LO7TzZH0Q5_aE8aGNmTiXZh6kKErYFrtXNYSDv13FrN2NElXabVV4fNrZol7LAaVb1gJj9lr",
    -            environment = Environment.SANDBOX,
    -            returnUrl = "${BuildConfig.APPLICATION_ID}://paypalpay"
    -        )
    -        PayPalCheckout.setConfig(config)
        }
    }

    class MainActivity : AppCompatActivity() {

         fun configurePayPalCheckout() {
    +        val coreConfig = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)

    +        paypalClient = PayPalNativeCheckoutClient(
    +           application = application,
    +           coreConfig = coreConfig,
    +           returnUrl = "${BuildConfig.APPLICATION_ID}://paypalpay"
    +        )
        }
    }
    ```

 3. Update your Button

    * Update your UI to display a `com.paypal.android.paymentbuttons.PayPalButton`, instead of a `com.paypal.checkout.paymentbutton.PaymentButtonContainer`.
    
    
    ```diff
    <?xml version="1.0" encoding="utf-8"?>
    <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".MainActivity">

    -    <com.paypal.checkout.paymentbutton.PaymentButtonContainer
    -        android:id="@+id/payment_button_container"
    -        android:layout_width="match_parent"
    -        android:layout_height="wrap_content"
    -        android:visibility="gone"
    -        app:paypal_button_shape="rectangle"
    -        app:paypal_button_size="large"
    -        app:paypal_button_enabled="true"
    -        tools:ignore="MissingConstraints"
    -        app:layout_constraintBottom_toBottomOf="parent"
    -        app:layout_constraintEnd_toEndOf="parent"
    -        app:layout_constraintStart_toStartOf="parent"
    -        app:layout_constraintTop_toTopOf="parent" />

    +    <com.paypal.android.paymentbuttons.PayPalButton
    +        android:id="@+id/paypal_button"
    +        android:layout_width="match_parent"
    +        android:layout_height="wrap_content"
    +        app:layout_constraintBottom_toBottomOf="parent"
    +        app:layout_constraintEnd_toEndOf="parent"
    +        app:layout_constraintStart_toStartOf="parent"
    +        app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
    ```

    ```diff
    class MainActivity : AppCompatActivity() {

    -   lateinit var paymentButtonContainer: PaymentButtonContainer
    +   lateinit var paypalButton: PayPalButton

        override fun onCreate(savedInstanceState: Bundle?) {
            ...

    -       paymentButtonContainer = findViewById(R.id.payment_button_container) as PaymentButtonContainer
    +       paypalButton = findViewById(R.id.paypal_button) as PayPalButton

    +       paypalButton.setOnClickListener {
    +           paypalButtonTapped()
    +       }
        }
    }
    ```
    
4. Implement an onClickListener

    * Create a `PayPalNativeCheckoutRequest` with your Order ID from the [pre-requisite](#pre-requisites) steps.
    * Call `PayPalNativeCheckoutClient.startCheckout()` to present the PayPal Paysheet.
    
    ```
        fun paypalButtonTapped() {
            val request = PayPalNativeCheckoutRequest("<ORDER_ID>")
            paypalClient.startCheckout(request)
        }
    ```

5. Implement listeners & remove `PayPalButtonContainers.setup` callbacks

     * Implement the required `PayPalNativeCheckoutListener`. This is how your app will receive notifications of the PayPal flow's success, cancel, error, and willStart events.
        * Remove the analogous callback methods set on your previous `PaymentButtonContainer`.
    
    ```diff
    private fun configurePayPalCheckout() {
    -    paymentButtonContainer.setup(
    -        createOrder = CreateOrder { createOrderActions ->
    -            createOrderActions.set(orderId!!)
    -        },
    -        onApprove = OnApprove {
    -            // Handle result of order approval (authorize or capture)
    -        },
    -        onCancel = OnCancel {
    -            // Handle cancel case
    -        },
    -        onError = OnError { errorInfo ->
    -            // Handle error case
    -        },
    -        onShippingChange = OnShippingChange { shippingChangeData, shippingChangeActions ->
    -            // Handle user shipping address & method selection change
    -        }
    -    )

    +    paypalClient.listener = object : PayPalNativeCheckoutListener {
    +        override fun onPayPalCheckoutStart() {
    +            // The PayPal paysheet is about to appear. Prepare your UI.
    +        }

    +        override fun onPayPalSuccess(result: PayPalNativeCheckoutResult) {
    +            // Handle result of order approval (authorize or capture)
    +        }

    +        override fun onPayPalFailure(error: PayPalSDKError) {
    +            // Handle error case
    +        }

    +        override fun onPayPalCanceled() {
    +            // Handle cancel case
    +        }
    +    }
    }
    ```
5. Implement shipping listener

    :warning: Only implement `PayPalNativeShippingListener` if your order ID was created with [`shipping_preference`](https://developer.paypal.com/docs/api/orders/v2/#definition-experience_context_base) = `GET_FROM_FILE`. If you created your order ID with `shipping_preference` = `NO_SHIPPING` or `SET_PROVIDED_ADDRESS`, **skip this step** (step 6).


    * `PayPalNativeShippingListener` notifies your app when the user updates their shipping address **or** shipping method. 
        * In the previous SDK, both shipping change types were lumped into one `onShippingChange`.
    * You are required to PATCH the order details on your server if the shipping method (or amount) changes. Do this with the [PayPal Orders API - Update order](https://developer.paypal.com/docs/api/orders/v2/#orders_patch) functionality.


    ```diff
    private fun configureShippingCallbacks() {
    +    paypalClient.shippingListener = object : PayPalNativeShippingListener {
    +        override fun onPayPalNativeShippingAddressChange(
    +            actions: PayPalNativePaysheetActions,
    +            shippingAddress: PayPalNativeShippingAddress
    +        ) {
    +            // called when the user updates their chosen shipping address

    +            // REQUIRED: you must call actions.approve() or actions.reject() in this callback
    +            actions.approve()

    +            // OPTIONAL: you can optionally patch your order. Once complete, call actions.approve() if successful or actions.reject() if not.
    +        }

    +        override fun onPayPalNativeShippingMethodChange(
    +            actions: PayPalNativePaysheetActions,
    +            shippingMethod: PayPalNativeShippingMethod
    +        ) {
    +           // called when the user updates their chosen shipping method

    +           // REQUIRED: patch your order server-side with the updated shipping amount.
    +           // Once complete, call `actions.approve()` or `actions.reject()`
    +           try {
    +               patchOrder()
    +               actions.approve()
    +           } else {
    +               actions.reject()
    +           }
    +        }
    +    }
    }
    ```
   
7. Remove the old SDK dependency

    Remove `com.paypal.checkout:android-sdk` from your app-level build.gradle file. Re-sync your project
 
