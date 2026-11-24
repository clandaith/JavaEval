package com.clicklease.app.bureau;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single-row table holding the cumulative paid bureau call count, so vendor
 * cost survives an app restart just like the applications do.
 */
@Entity
@Table(name = "bureau_stats")
public class BureauStats {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private long callCount;

    protected BureauStats() {
    }

    public BureauStats(Long id) {
        this.id = id;
        this.callCount = 0;
    }

    public long getCallCount() {
        return callCount;
    }

    public void setCallCount(long callCount) {
        this.callCount = callCount;
    }
}
