package com.mindq.payment.provider;

/**
 * Abstraction for payment providers (Razorpay, Stripe, etc).
 * Implementations should handle checkout session creation and webhook verification.
 */
public interface PaymentProvider {

    /**
     * Create a checkout/order for a subscription plan.
     * Returns the provider's order/session ID for the client to complete payment.
     */
    CheckoutResult createOrder(String userId, String planCode, long amountPaise, String currency);

    /**
     * Verify a webhook signature to ensure it came from the provider.
     */
    boolean verifyWebhookSignature(String payload, String signature, String secret);

    /**
     * Extract the order/payment ID from a webhook payload.
     */
    String extractPaymentId(String webhookPayload);

    /**
     * Extract the payment status from a webhook payload.
     * Returns: SUCCESS, FAILED, PENDING, REFUNDED
     */
    String extractStatus(String webhookPayload);

    /**
     * Get the provider name.
     */
    String getProviderName();

    record CheckoutResult(
            String orderId,
            String amountCurrency,
            long amountPaise,
            String razorpayKey
    ) {}
}
