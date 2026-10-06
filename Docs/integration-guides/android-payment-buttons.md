The SDK ships PayPal-branded buttons you drop into your layout — `PayPalButton`, `PayLaterButton`, and `PayPalCreditButton` (the `payment-buttons` module). Use these rather than building your own: they carry PayPal's wordmark, brand colors, typography (the PayPalOpen font), and accessibility. This guide covers adding, styling, and wiring a button; for the checkout it triggers, see [PayPal Checkout](android-paypal-checkout.md).

> **Before you begin:** complete [Install & Setup (Android)](../getting-started/android-install-and-setup.md) and add the `com.paypal.android:payment-buttons` module.

## Add a button

Declare the button in your layout — it renders without an order or session:

```xml
<com.paypal.android.paymentbuttons.PayPalButton
    android:id="@+id/payPalButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Or create it programmatically with `PayPalButton(context)`.

## Customize

Set these in code or via the XML attribute. `PayPalButton` supports color, label, shape, and size.

| Property (code) | XML attribute | Values | Default |
| --- | --- | --- | --- |
| `color` | `paypal_color` | `PayPalButtonColor`: `GOLD`, `BLUE`, `WHITE`, `BLACK`, `SILVER` | `GOLD` |
| `label` | `paypal_label` | `PayPalButtonLabel`: `PAYPAL` (wordmark only), `CHECKOUT`, `BUY_NOW`, `PAY` | `PAYPAL` |
| `shape` | `payment_button_shape` | `PaymentButtonShape`: `ROUNDED`, `PILL`, `RECTANGLE` | `ROUNDED` (or your Material theme's shape) |
| `size` | `payment_button_size` | `PaymentButtonSize` (defaults to `MEDIUM`) | `MEDIUM` |
| `customCornerRadius` | — | `Float` (dp) — alternative to `shape`; cannot be combined with it | — |

```kotlin
payPalButton.color = PayPalButtonColor.GOLD
payPalButton.label = PayPalButtonLabel.CHECKOUT
payPalButton.shape = PaymentButtonShape.PILL
```

Notes:

* `GOLD` is the recommended default — PayPal's research shows it converts best. `BLUE` is the preferred alternative; `WHITE`, `BLACK`, and `SILVER` are secondary.
* `WHITE` renders with an outline for contrast.
* Stick to these built-in options rather than recoloring or rebuilding the button — they keep you brand-compliant.
* For Pay Later or PayPal Credit, use `PayLaterButton` or `PayPalCreditButton`. `PayLaterButton` shares `PayPalButton`'s color enum (`PayPalButtonColor`) and also exposes a `paylater_color` XML attribute; `PayPalCreditButton` has its own distinct enum, `PayPalCreditButtonColor` (`DARK_BLUE`, `BLACK`, `GOLD`, `WHITE`).

## Handle taps

Attach a click listener and start checkout from it:

```kotlin
payPalButton.setOnClickListener { beginCheckout() }
```

What happens in `beginCheckout()` — preparing the session, creating the order, and calling `start()` — is covered in [PayPal Checkout — Integration Guide (Android)](android-paypal-checkout.md). For Pay Later / PayPal Credit funding, see that guide's _Pay Later and PayPal Credit_ section.

## Accessibility and localization

`PayPalButton` and `PayPalCreditButton` set their own content description so screen readers announce them correctly; `PayLaterButton` currently does not set one — add your own via `android:contentDescription` if you use it. The module ships English strings only today, with no localized string resources. Make sure your layout gives the button an adequate touch target and sufficient contrast against its background.

## Related

* [Install & Setup (Android)](../getting-started/android-install-and-setup.md)
* [PayPal Checkout — Integration Guide (Android)](android-paypal-checkout.md)
* [Troubleshooting (Android)](android-troubleshooting.md)
