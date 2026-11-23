package com.clicklease.app.service;

import com.clicklease.app.domain.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Required test #1: decision rules return the right result at the boundaries
 * (649/650, 25,000/25,001).
 */
class DecisionServiceTest {

    private final DecisionService service = new DecisionService();

    @ParameterizedTest(name = "score={0}, amount={1} -> {2}")
    @CsvSource({
            // Lower tier boundary: score 650, amount <= 25,000
            "649, 25000, DECLINED",
            "650, 25000, APPROVED",
            "651, 25000, APPROVED",
            // Amount boundary at exactly / just over 25,000
            "650, 25000.00, APPROVED",
            "650, 25000.01, DECLINED",
            "650, 25001, DECLINED",
            // Upper tier boundary: amount > 25,000 needs score >= 700
            "699, 25001, DECLINED",
            "699, 50000, DECLINED",
            "700, 25001, APPROVED",
            "700, 50000, APPROVED",
            "850, 50000, APPROVED",
            // Score 700+ approves either tier
            "700, 25000, APPROVED",
            "800, 25000, APPROVED",
            // Low scores always decline
            "300, 500, DECLINED",
            "649, 500, DECLINED",
            // Amount range edges
            "650, 500, APPROVED",
            "650, 50000, DECLINED"
    })
    void decisionRulesAtBoundaries(int score, String amount, ApplicationStatus expected) {
        assertEquals(expected, service.decide(score, new BigDecimal(amount)).status());
    }

    @Test
    void decisionIncludesReason() {
        var decision = service.decide(700, new BigDecimal("40000"));
        assertEquals(ApplicationStatus.APPROVED, decision.status());
        assertEquals(true, decision.reason() != null && decision.reason().contains("700"));
    }
}
