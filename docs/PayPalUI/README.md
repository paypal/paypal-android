# Pay using PayPal UI

1. [Add PayPal UI](#add-paypal-ui)

## Add PayPal UI

### 1. Add PayPalUI to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-ui:1.0.0"
}
```

### 2. Create a PayPal button
The PayPalUI module allows you to render three buttons that can offer a set of customizations like color, edges, size and labels:
* `PayPalButton`: generic PayPal button
* `PayPalPayLater`: a PayPal button with a fixed PayLater label
* `PayPalCredit`: a PayPal button with the PayPalCredit logo

Add a `PayPalButton` to your layout XML:

```xml
<com.paypal.android.ui.paymentbutton.PayPalButton
    android:id="@+id/paypal_button"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```
### 3. Reference the PayPalButton

Reference the button in your code:

```kotlin
val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)

payPalButton.setOnClickListener {
    // start the paypal flow
}
```