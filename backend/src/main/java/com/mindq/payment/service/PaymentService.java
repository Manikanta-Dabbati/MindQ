package com.mindq.payment.service;

import com.mindq.model.Plan;
import com.mindq.model.User;
import com.mindq.model.UserSubscription;
import com.mindq.model.PaymentTransaction;
import com.mindq.payment.provider.PaymentProvider;
import com.mindq.payment.provider.RazorpayProvider;
import com.mindq.repository.PlanRepository;
import com.mindq.repository.UserRepository;
import com.mindq.repository.UserSubscriptionRepository;
import com.mindq.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayProvider razorpayProvider;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.payment.razorpay.enabled:false}")
    private boolean paymentEnabled;

    /**
     * Create a checkout order for upgrading to a new plan.
     */
    @Transactional
    public PaymentProvider.CheckoutResult createCheckoutOrder(String email, String planCode, String billingPeriod) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan: " + planCode));

        if ("FREE".equals(planCode)) {
            throw new IllegalArgumentException("Cannot purchase FREE plan");
        }

        long amountPaise = billingPeriod.equals("YEARLY")
                ? (long) plan.getPriceInPaise() * 10
                : plan.getPriceInPaise();

        // Create order
        PaymentProvider.CheckoutResult result = razorpayProvider.createOrder(
                String.valueOf(user.getId()),
                planCode,
                amountPaise,
                "INR"
        );

        // Record the pending transaction
        PaymentTransaction txn = PaymentTransaction.builder()
                .user(user)
                .plan(plan)
                .provider("RAZORPAY")
                .providerOrderId(result.orderId())
                .amountPaise(amountPaise)
                .currency("INR")
                .status("PENDING")
                .billingPeriod(billingPeriod)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMonths(billingPeriod.equals("YEARLY") ? 12 : 1))
                .build();
        transactionRepository.save(txn);

        log.info("Created checkout order {} for user {}, plan {}, amount {} paise",
                result.orderId(), email, planCode, amountPaise);

        return result;
    }

    /**
     * Process a successful payment webhook from Razorpay.
     * Activates the subscription.
     */
    @Transactional
    public void processPaymentSuccess(String orderId, String paymentId) {
        PaymentTransaction txn = transactionRepository.findByProviderOrderId(orderId)
                .orElse(null);

        if (txn == null) {
            log.warn("No transaction found for order {}", orderId);
            return;
        }

        if ("SUCCESS".equals(txn.getStatus())) {
            log.info("Transaction {} already processed", orderId);
            return; // Idempotent
        }

        // Update transaction
        txn.setProviderPaymentId(paymentId);
        txn.setStatus("SUCCESS");
        transactionRepository.save(txn);

        // Activate subscription
        User user = txn.getUser();
        Plan plan = txn.getPlan();

        // Deactivate any existing subscription
        subscriptionRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")
                .ifPresent(existing -> {
                    existing.setStatus("SUPERSEDED");
                    existing.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(existing);
                });

        // Create new subscription
        UserSubscription sub = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status("ACTIVE")
                .startDate(txn.getValidFrom())
                .endDate(txn.getValidUntil())
                .build();
        subscriptionRepository.save(sub);

        log.info("Activated {} subscription for user {} via payment {}", plan.getCode(), user.getEmail(), paymentId);
    }

    /**
     * Process a failed payment webhook from Razorpay.
     */
    @Transactional
    public void processPaymentFailure(String orderId, String failureReason) {
        PaymentTransaction txn = transactionRepository.findByProviderOrderId(orderId)
                .orElse(null);

        if (txn == null) {
            log.warn("No transaction found for failed order {}", orderId);
            return;
        }

        txn.setStatus("FAILED");
        txn.setFailureReason(failureReason);
        transactionRepository.save(txn);

        log.info("Payment failed for order {}: {}", orderId, failureReason);
    }

    /**
     * Get user's payment history.
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentTransaction> getPaymentHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return transactionRepository.findByUserIdWithPlanOrderByCreatedAtDesc(user.getId());
    }
}
