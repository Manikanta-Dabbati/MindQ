package com.mindq.common.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
public class MetricsService {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    private final LongAdder aiRequests = new LongAdder();
    private final LongAdder aiSuccesses = new LongAdder();
    private final LongAdder aiFailures = new LongAdder();
    private final LongAdder aiRetries = new LongAdder();
    private final AtomicLong aiTotalLatencyMs = new AtomicLong(0);
    private final AtomicLong aiMaxLatencyMs = new AtomicLong(0);
    private final ConcurrentHashMap<String, LongAdder> aiFailureByType = new ConcurrentHashMap<>();

    private final LongAdder loginAttempts = new LongAdder();
    private final LongAdder loginSuccesses = new LongAdder();
    private final LongAdder loginFailures = new LongAdder();
    private final LongAdder registrations = new LongAdder();
    private final LongAdder passwordResets = new LongAdder();
    private final LongAdder accountDeletions = new LongAdder();

    private final LongAdder uploads = new LongAdder();
    private final AtomicLong totalUploadBytes = new AtomicLong(0);

    private final LongAdder quizzesGenerated = new LongAdder();
    private final LongAdder quizzesSubmitted = new LongAdder();

    public void recordRequest(boolean success) {
        totalRequests.increment();
        if (success) successfulRequests.increment(); else failedRequests.increment();
    }

    public void recordAiRequest(long latencyMs, boolean success) {
        aiRequests.increment();
        if (success) aiSuccesses.increment(); else aiFailures.increment();
        aiTotalLatencyMs.addAndGet(latencyMs);
        long currentMax;
        do { currentMax = aiMaxLatencyMs.get(); if (latencyMs <= currentMax) return; } while (!aiMaxLatencyMs.compareAndSet(currentMax, latencyMs));
    }

    public void recordAiRetry(String reason) {
        aiRetries.increment();
        aiFailureByType.computeIfAbsent(reason, k -> new LongAdder()).increment();
    }

    public void recordLoginAttempt(boolean success) {
        loginAttempts.increment();
        if (success) loginSuccesses.increment(); else loginFailures.increment();
    }

    public void recordRegistration() { registrations.increment(); }
    public void recordPasswordReset() { passwordResets.increment(); }
    public void recordAccountDeletion() { accountDeletions.increment(); }
    public void recordUpload(long bytes) { uploads.increment(); totalUploadBytes.addAndGet(bytes); }
    public void recordQuizGenerated() { quizzesGenerated.increment(); }
    public void recordQuizSubmitted() { quizzesSubmitted.increment(); }

    public MetricsSnapshot getSnapshot() {
        long aiReqs = aiRequests.sum();
        double avgLatency = aiReqs > 0 ? (double) aiTotalLatencyMs.get() / aiReqs : 0;

        Map<String, Long> failureMap = new HashMap<>();
        aiFailureByType.forEach((k, v) -> failureMap.put(k, v.sum()));

        return MetricsSnapshot.builder()
                .requests(TotalRequests.builder().total(totalRequests.sum()).successful(successfulRequests.sum()).failed(failedRequests.sum()).build())
                .ai(AiMetrics.builder().totalRequests(aiReqs).successes(aiSuccesses.sum()).failures(aiFailures.sum()).retries(aiRetries.sum()).avgLatencyMs(Math.round(avgLatency)).maxLatencyMs(aiMaxLatencyMs.get()).failureBreakdown(failureMap).build())
                .auth(AuthMetrics.builder().loginAttempts(loginAttempts.sum()).loginSuccesses(loginSuccesses.sum()).loginFailures(loginFailures.sum()).registrations(registrations.sum()).passwordResets(passwordResets.sum()).accountDeletions(accountDeletions.sum()).build())
                .storage(StorageMetrics.builder().uploads(uploads.sum()).totalBytes(totalUploadBytes.get()).build())
                .quiz(QuizMetrics.builder().generated(quizzesGenerated.sum()).submitted(quizzesSubmitted.sum()).build())
                .build();
    }

    @lombok.Builder @lombok.Data
    public static class MetricsSnapshot { private TotalRequests requests; private AiMetrics ai; private AuthMetrics auth; private StorageMetrics storage; private QuizMetrics quiz; }

    @lombok.Builder @lombok.Data
    public static class TotalRequests { private long total; private long successful; private long failed; }

    @lombok.Builder @lombok.Data
    public static class AiMetrics { private long totalRequests; private long successes; private long failures; private long retries; private long avgLatencyMs; private long maxLatencyMs; private Map<String, Long> failureBreakdown; }

    @lombok.Builder @lombok.Data
    public static class AuthMetrics { private long loginAttempts; private long loginSuccesses; private long loginFailures; private long registrations; private long passwordResets; private long accountDeletions; }

    @lombok.Builder @lombok.Data
    public static class StorageMetrics { private long uploads; private long totalBytes; }

    @lombok.Builder @lombok.Data
    public static class QuizMetrics { private long generated; private long submitted; }
}
