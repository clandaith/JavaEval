package com.clicklease.app.bureau;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The paid-call counter is persisted in bureau_stats, so the vendor cost is
 * accurate across restarts and the count reads back from the database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BureauCallRegistryIT {

    @Autowired
    private BureauCallRegistry registry;

    @Autowired
    private BureauStatsRepository statsRepository;

    @Test
    void callCountIsPersistedInDatabase() {
        long before = registry.getCallCount();

        registry.recordCall();
        registry.recordCall();

        assertEquals(before + 2, registry.getCallCount());
        // Read straight from the table — proves persistence, not just memory.
        assertEquals(registry.getCallCount(),
                statsRepository.findById(BureauStats.SINGLETON_ID).orElseThrow().getCallCount());
        assertEquals(new BigDecimal("1.50"), registry.getCallCost());
        assertEquals(registry.getCallCost().multiply(BigDecimal.valueOf(registry.getCallCount())),
                registry.getTotalCost());
    }
}
