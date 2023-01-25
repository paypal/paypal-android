# Pay using Payment Buttons

1. [Add Payment Buttons](#add-payment-buttons)

## Add Payment Buttons

### 1. Add PaymentButtons to your app

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/payment-buttons?style=for-the-badge) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/payment-buttons?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:payment-buttons:<CURRENT-VERSION>"
}
```

### 2. Create a PayPal button
The PaymentButtons module allows you to render three buttons that can offer a set of customizations like color, edges, size and labels:
* `PayPalButton`: generic PayPal button
* `PayLaterButton`: a PayPal button with a fixed PayLater label
* `PayPalCreditButton`: a PayPal button with the PayPalCredit logo

Add a `PayPalButton` to your layout XML:

```xml
<com.paypal.android.paymentbuttons.PayPalButton
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