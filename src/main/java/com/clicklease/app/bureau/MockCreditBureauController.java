package com.clicklease.app.bureau;

import com.clicklease.app.util.TaxIdMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fake credit bureau vendor endpoint. Deterministic score per tax ID,
 * random 200ms-3s latency, ~20% random failures, and per-call billing
 * tracked in {@link BureauCallRegistry}.
 */
@RestController
@RequestMapping("/mock/bureau")
public class MockCreditBureauController {

    private static final Logger logger = LoggerFactory.getLogger(MockCreditBureauController.class);
    private static final int MIN_SCORE = 300;
    private static final int SCORE_RANGE = 551; // 300..850 inclusive

    private final BureauCallRegistry registry;
    private final double failureRate;
    private final long minDelayMs;
    private final long maxDelayMs;

    public MockCreditBureauController(BureauCallRegistry registry,
                                      @Value("${bureau.failure-rate:0.20}") double failureRate,
                                      @Value("${bureau.min-delay-ms:200}") long minDelayMs,
                                      @Value("${bureau.max-delay-ms:3000}") long maxDelayMs) {
        this.registry = registry;
        this.failureRate = failureRate;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * POST so the sensitive tax ID travels in the request body, never in a
     * URL that could end up in an access log.
     */
    @PostMapping("/score")
    public ResponseEntity<Map<String, Object>> getScore(@RequestBody ScoreRequest request) {
        String taxId = request.taxId();
        long callNumber = registry.recordCall();
        logger.info("DEBUG MSG: Bureau received score request #{} for taxId={}",
                callNumber, TaxIdMasker.mask(taxId));

        simulateLatency();

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            logger.info("DEBUG MSG: Bureau simulating vendor failure on call #{}", callNumber);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "credit bureau unavailable"));
        }

        int score = deterministicScore(taxId);
        logger.info("DEBUG MSG: Bureau returning score {} for taxId={}", score, TaxIdMasker.mask(taxId));
        return ResponseEntity.ok(Map.of("score", score));
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
                "calls", registry.getCallCount(),
                "costPerCall", registry.getCallCost(),
                "totalCost", registry.getTotalCost());
    }

    public record ScoreRequest(String taxId) {
    }

    static int deterministicScore(String taxId) {
        return MIN_SCORE + Math.floorMod(taxId.hashCode(), SCORE_RANGE);
    }

    private void simulateLatency() {
        long delay = ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
