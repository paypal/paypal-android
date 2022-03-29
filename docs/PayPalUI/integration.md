---
title: Use PayPal UI Components Integration
keywords:
contentType: docs
productStatus: current
apiVersion: TODO
sdkVersion: TODO
---
### 1. Add PayPalUI to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-ui:1.0.0"
}
```

### 2. Create a PayPal button

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
    //insert your code here, like starting the paypal flow
}
```