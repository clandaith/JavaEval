package com.clicklease.app.bureau;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BureauCallRegistry {

    private final AtomicInteger callCount = new AtomicInteger();
    private final BigDecimal callCost;

    public BureauCallRegistry(@Value("${bureau.call-cost:1.50}") BigDecimal callCost) {
        this.callCost = callCost;
    }

    public int recordCall() {
        return callCount.incrementAndGet();
    }

    public int getCallCount() {
        return callCount.get();
    }

    public BigDecimal getCallCost() {
        return callCost;
    }

    public BigDecimal getTotalCost() {
        return callCost.multiply(BigDecimal.valueOf(callCount.get()));
    }
}
