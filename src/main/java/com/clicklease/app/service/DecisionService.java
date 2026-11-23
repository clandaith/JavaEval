package com.clicklease.app.service;

import com.clicklease.app.domain.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DecisionService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);
    private static final BigDecimal TIER_LIMIT = new BigDecimal("25000");
    private static final int LOW_TIER_MIN_SCORE = 650;
    private static final int HIGH_TIER_MIN_SCORE = 700;

    public record Decision(ApplicationStatus status, String reason) {
    }

    public Decision decide(int creditScore, BigDecimal amount) {
        boolean smallLease = amount.compareTo(TIER_LIMIT) <= 0;
        boolean approved = (smallLease && creditScore >= LOW_TIER_MIN_SCORE)
                || (!smallLease && creditScore >= HIGH_TIER_MIN_SCORE);

        String reason = approved
                ? String.format("Credit score %d meets the minimum for a lease of $%s",
                        creditScore, amount.toPlainString())
                : String.format("Credit score %d below required minimum for a lease of $%s",
                        creditScore, amount.toPlainString());

        logger.info("DEBUG MSG: Decision for score={} amount={} -> {}",
                creditScore, amount, approved ? "APPROVED" : "DECLINED");
        return new Decision(approved ? ApplicationStatus.APPROVED : ApplicationStatus.DECLINED, reason);
    }

    public Decision pending(String detail) {
        return new Decision(ApplicationStatus.PENDING, "Credit bureau unavailable: " + detail);
    }
}
