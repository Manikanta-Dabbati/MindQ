# Subscriptions & Payments

## Plans

The project includes tiered plans:

- FREE
- PRO
- PREMIUM

Plan data is modeled through dedicated plan/subscription entities.

## Entitlements

The entitlement architecture is designed to control:

- storage
- AI usage
- question limits
- advanced features

Subscription state must always be evaluated on the backend.

## Razorpay

Razorpay is integrated through a payment-provider abstraction.

Flow:

```text
Select Plan
 ↓
Create Order
 ↓
Razorpay Checkout
 ↓
Payment
 ↓
Server Verification
 ↓
Webhook
 ↓
Subscription
 ↓
Entitlements
```

## Security

Never activate a subscription solely because the frontend reports success.

Payment signatures and webhook events must be verified server-side.

Payment operations should be idempotent.

Do not store card numbers, CVV or other raw card credentials.

## Billing

Payment records contain provider/order/payment identifiers, plan, amount, status and billing information required by the application.

