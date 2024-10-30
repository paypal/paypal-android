# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. CardPayments
1. PayPalWebPayments
1. PayPalNativePayments

### CardPayments

We have refactored the `CardClient` API to improve the developer experience.

#### Activity Reference no Longer Required in CardClient Constructor

The `CardClient` constructor no longer requires an activity reference. We require an activity reference only when we're launching a Chrome Custom Tab, e.g. `CardClient.presentAuthChallenge()`.

This should make constructing `CardClient` instances less restrictive e.g. it should easier to construct a `CardClient` within a Jetpack `ViewModel`:

```diff
val config = CoreConfig("<CLIENT_ID>", Environment.LIVE)
-val cardClient = CardClient(requireActivity(), config)
+val cardClient = CardClient(requireContext(), config)
```

### PayPalWebPayments

We have refactored the `PayPalWebClient` API to improve the developer experience.

### PayPalNativePayments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
