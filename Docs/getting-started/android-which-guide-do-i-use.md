Use this to find the right page in the PayPal Mobile SDK V3.0.0 Android guide set.

## What are you trying to do?

| If you want to… | Go to |
| --- | --- |
| Understand what the SDK is and which methods and flows are supported | [Overview](../README.md) |
| Understand how the pieces fit and who is responsible for what | [Concepts & Responsibilities](android-concepts-and-responsibilities.md) |
| Get the SDK installed and initialized | **[Install & Setup](../integration-guides/android-install-and-setup.md)** — do this first |
| Accept a **PayPal** payment (incl. vault, Pay Later / PayPal Credit) | [PayPal Checkout](../integration-guides/android-paypal-checkout.md) |
| Accept a **Venmo** payment | [Venmo Checkout](../integration-guides/android-venmo-checkout.md) |
| Accept a **card** payment / ACDC (incl. vault) | [Card / ACDC](../integration-guides/android-card-acdc.md) |
| Move an existing 2.x integration to V3 | [Migrate V2 → V3](../android-migrate-v2-to-v3.md) |
| Diagnose a build or integration failure | [Troubleshooting](../integration-guides/android-troubleshooting.md) |

## Quick answers

**Do I need all three payment methods?** No — each is independent. They all share the same Install & Setup; add only the modules and guides for the methods you support.

**Do I build my own PayPal button?** Use the PayPal-provided button — it's the supported, brand-compliant way to start PayPal (and Venmo) checkout. See PayPal Checkout.

**Does Card have a button?** No. The SDK ships no card UI — you build your own card fields and pass them to `CardClient`. See Card / ACDC.

**Which methods support vault?** PayPal and Card support vault (with and without purchase); Venmo is one-time only.

**Where's the full class and method reference?** It's generated from the SDK source (Dokka) and linked from each guide — we don't hand-maintain it here.

## Start

Do [Install & Setup](../integration-guides/android-install-and-setup.md), then the method guide you need.
