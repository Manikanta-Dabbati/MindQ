package com.mindq.payment.controller;

import com.mindq.payment.provider.RazorpayProvider;
import com.mindq.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles payment webhook callbacks from Razorpay.
 * This endpoint must be publicly accessible (no auth) — security is via webhook signature.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RazorpayProvider razorpayProvider;
    private final PaymentService paymentService;

    @Value("${app.payment.razorpay.secret:placeholder_secret}")
    private String razorpaySecret;

    @Value("${app.payment.razorpay.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.payment.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    /**
     * Razorpay webhook endpoint.
     *
     * Razorpay sends:
     * - Header: X-Razorpay-Signature
     * - Body: JSON event payload
     *
     * Events handled:
     * - payment.captured → activate subscription
     * - payment.failed → mark transaction as failed
     */
    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("Received Razorpay webhook from IP {}", request.getRemoteAddr());

        // Skip processing entirely when Razorpay is disabled (dev/test mode)
        if (!razorpayEnabled) {
            log.warn("Razorpay is disabled — webhook ignored");
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Razorpay disabled"));
        }

        // Reject empty payloads early
        if (payload == null || payload.isBlank()) {
            log.warn("Empty webhook payload from IP {}", request.getRemoteAddr());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Empty payload"));
        }

        // Verify signature in production — reject if missing or invalid
        if (signature == null || signature.isBlank()) {
            log.warn("Missing Razorpay webhook signature from IP {}", request.getRemoteAddr());
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Missing signature"));
        }

        // Use webhook secret if configured, otherwise fall back to API secret
        String secretToUse = (webhookSecret != null && !webhookSecret.isBlank()) ? webhookSecret : razorpaySecret;
        if (!razorpayProvider.verifyWebhookSignature(payload, signature, secretToUse)) {
            log.warn("Invalid Razorpay webhook signature from IP {}", request.getRemoteAddr());
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", "Invalid signature"));
        }

        try {
            String eventType = extractEventType(payload);
            String orderId = extractOrderId(payload);
            String paymentId = razorpayProvider.extractPaymentId(payload);
            String status = razorpayProvider.extractStatus(payload);

            log.info("Razorpay event: {}, order: {}, payment: {}, status: {}", eventType, orderId, paymentId, status);

            if ("SUCCESS".equals(status) && orderId != null) {
                paymentService.processPaymentSuccess(orderId, paymentId);
            } else if ("FAILED".equals(status) && orderId != null) {
                paymentService.processPaymentFailure(orderId, "Payment failed via webhook");
            } else {
                log.info("Ignoring Razorpay event: {}", eventType);
            }

            return ResponseEntity.ok(Map.of("status", "ok"));

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Internal error"));
        }
    }

    private String extractEventType(String payload) {
        try {
            var mapper = new tools.jackson.databind.ObjectMapper();
            var json = mapper.readTree(payload);
            return json.path("event").asText("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractOrderId(String payload) {
        try {
            var mapper = new tools.jackson.databind.ObjectMapper();
            var json = mapper.readTree(payload);
            return json.path("payload").path("payment").path("entity").path("order_id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
