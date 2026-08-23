package com.mindq.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Razorpay payment provider for Indian payments.
 *
 * When enabled, creates real orders via the Razorpay Orders API.
 * When disabled, returns mock orders for local development.
 */
@Slf4j
@Component
public class RazorpayProvider implements PaymentProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.payment.razorpay.key:rzp_test_placeholder}")
    private String razorpayKey;

    @Value("${app.payment.razorpay.secret:placeholder_secret}")
    private String razorpaySecret;

    @Value("${app.payment.razorpay.enabled:false}")
    private boolean enabled;

    @Override
    public CheckoutResult createOrder(String userId, String planCode, long amountPaise, String currency) {
        if (!enabled) {
            // Dev/test mode -- return a mock order with placeholder key
            // The frontend detects "rzp_test_placeholder" to trigger test-confirm flow
            String mockOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("Razorpay disabled -- returning mock order {} for user {}", mockOrderId, userId);
            return new CheckoutResult(mockOrderId, currency, amountPaise, "rzp_test_placeholder");
        }

        try {
            // Call Razorpay Orders API: POST https://api.razorpay.com/v1/orders
            String auth = Base64.getEncoder().encodeToString(
                    (razorpayKey + ":" + razorpaySecret).getBytes(StandardCharsets.UTF_8));

            String body = MAPPER.writeValueAsString(Map.of(
                    "amount", amountPaise,
                    "currency", currency,
                    "receipt", "mindq_" + userId + "_" + planCode,
                    "notes", Map.of("plan", planCode, "user_id", userId)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + auth)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode json = MAPPER.readTree(response.body());
                String orderId = json.path("id").asText();
                log.info("Created Razorpay order {} for user {}, plan {}, amount {} paise",
                        orderId, userId, planCode, amountPaise);
                return new CheckoutResult(orderId, currency, amountPaise, razorpayKey);
            } else {
                log.error("Razorpay order creation failed: HTTP {} -- {}", response.statusCode(), response.body());
                // Fall back to mock order so the flow doesn't break completely
                String fallbackOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
                log.warn("Falling back to mock order {} due to API error", fallbackOrderId);
                return new CheckoutResult(fallbackOrderId, currency, amountPaise, razorpayKey);
            }

        } catch (Exception e) {
            log.error("Failed to create Razorpay order via API", e);
            String fallbackOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.warn("Falling back to mock order {} due to exception", fallbackOrderId);
            return new CheckoutResult(fallbackOrderId, currency, amountPaise, razorpayKey);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (!enabled) {
            log.debug("Razorpay disabled -- skipping webhook verification");
            return true;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computedHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(computedHmac);
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to verify Razorpay webhook signature", e);
            return false;
        }
    }

    @Override
    public String extractPaymentId(String webhookPayload) {
        try {
            JsonNode json = MAPPER.readTree(webhookPayload);
            JsonNode payload = json.path("payload").path("payment").path("entity");
            return payload.path("id").asText(null);
        } catch (Exception e) {
            log.error("Failed to extract payment ID from Razorpay webhook", e);
            return null;
        }
    }

    @Override
    public String extractStatus(String webhookPayload) {
        try {
            JsonNode json = MAPPER.readTree(webhookPayload);
            String event = json.path("event").asText("");

            // Razorpay events: payment.captured, payment.failed, payment.authorized
            return switch (event) {
                case "payment.captured" -> "SUCCESS";
                case "payment.failed" -> "FAILED";
                case "payment.authorized" -> "PENDING";
                default -> "UNKNOWN";
            };
        } catch (Exception e) {
            log.error("Failed to extract status from Razorpay webhook", e);
            return "UNKNOWN";
        }
    }

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    public String getRazorpayKey() {
        return razorpayKey;
    }
}
