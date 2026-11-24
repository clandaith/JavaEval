package com.clicklease.app.bureau;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP-level coverage of the mocked vendor endpoint itself: deterministic
 * score per tax ID, per-call billing, and the stats endpoint shape.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "bureau.failure-rate=0.0",
        "bureau.min-delay-ms=1",
        "bureau.max-delay-ms=5"
})
class MockBureauEndpointIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BureauCallRegistry registry;

    @Test
    @SuppressWarnings("unchecked")
    void sameTaxIdReturnsSameScoreOverHttp() {
        var request = new MockCreditBureauController.ScoreRequest("12-3456789");

        Map<String, Object> first = restTemplate.postForObject(
                "/mock/bureau/score", request, Map.class);
        Map<String, Object> second = restTemplate.postForObject(
                "/mock/bureau/score", request, Map.class);

        assertNotNull(first);
        assertEquals(first.get("score"), second.get("score"));
        assertEquals(MockCreditBureauController.deterministicScore("12-3456789"),
                ((Number) first.get("score")).intValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyCallIsCountedAndStatsAreConsistent() {
        long before = registry.getCallCount();

        restTemplate.postForObject("/mock/bureau/score",
                new MockCreditBureauController.ScoreRequest("33-3333333"), Map.class);
        restTemplate.postForObject("/mock/bureau/score",
                new MockCreditBureauController.ScoreRequest("44-4444444"), Map.class);

        assertEquals(before + 2, registry.getCallCount());

        Map<String, Object> stats = restTemplate.getForObject("/mock/bureau/stats", Map.class);
        assertNotNull(stats);
        long calls = ((Number) stats.get("calls")).longValue();
        BigDecimal costPerCall = new BigDecimal(stats.get("costPerCall").toString());
        BigDecimal totalCost = new BigDecimal(stats.get("totalCost").toString());
        assertTrue(calls >= 2);
        assertEquals(0, new BigDecimal("1.50").compareTo(costPerCall));
        assertEquals(costPerCall.multiply(BigDecimal.valueOf(calls)), totalCost);
    }
}
