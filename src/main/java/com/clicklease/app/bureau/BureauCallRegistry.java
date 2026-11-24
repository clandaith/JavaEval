package com.clicklease.app.bureau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Counts paid vendor calls. Persisted in the bureau_stats table so the count
 * (and therefore the dollar cost) survives an app restart.
 */
@Component
public class BureauCallRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BureauCallRegistry.class);

    private final BureauStatsRepository repository;
    private final BigDecimal callCost;

    public BureauCallRegistry(BureauStatsRepository repository,
                              @Value("${bureau.call-cost:1.50}") BigDecimal callCost) {
        this.repository = repository;
        this.callCost = callCost;
    }

    public synchronized long recordCall() {
        BureauStats stats = repository.findById(BureauStats.SINGLETON_ID)
                .orElseGet(() -> new BureauStats(BureauStats.SINGLETON_ID));
        stats.setCallCount(stats.getCallCount() + 1);
        repository.save(stats);
        logger.info("DEBUG MSG: Paid bureau call recorded — lifetime calls={}",
                stats.getCallCount());
        return stats.getCallCount();
    }

    public long getCallCount() {
        return repository.findById(BureauStats.SINGLETON_ID)
                .map(BureauStats::getCallCount)
                .orElse(0L);
    }

    public BigDecimal getCallCost() {
        return callCost;
    }

    public BigDecimal getTotalCost() {
        return callCost.multiply(BigDecimal.valueOf(getCallCount()));
    }
}
