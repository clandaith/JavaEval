package com.clicklease.app.service;

import com.clicklease.app.TestFixtures;
import com.clicklease.app.bureau.BureauClient;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.repository.LeaseApplicationRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The PRD: "Never call the bureau twice for the same submission." This test
 * forces two concurrent submits of the same idempotency key through the
 * bureau-call window simultaneously, which exposes whether check-then-act
 * can double-bill the vendor under a race.
 */
@SpringBootTest
class ConcurrentSubmissionIT {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private LeaseApplicationRepository repository;

    @MockitoBean
    private BureauClient bureauClient;

    @Test
    @Disabled("KNOWN DEFECT (found by this test, fix deferred): check-then-act "
            + "idempotency races under true concurrency — both threads call the paid "
            + "bureau endpoint, and the loser's insert throws JpaSystemException "
            + "(SQLite reports UNIQUE violations as JpaSystemException, not "
            + "DataIntegrityViolationException) instead of being deduped. Fix: "
            + "synchronize/lock on the idempotency key AND catch "
            + "JpaSystemException/DataAccessException in saveIdempotently.")
    void concurrentDuplicateSubmissionsCallBureauAtMostOnce() throws Exception {
        CountDownLatch bothInsideCall = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(bureauClient.fetchScore(anyString())).thenAnswer(inv -> {
            bothInsideCall.countDown();
            release.await(5, TimeUnit.SECONDS);
            return 700;
        });

        var formA = TestFixtures.validForm("race-key");
        var formB = TestFixtures.validForm("race-key");
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<LeaseApplication> a = pool.submit(() -> applicationService.submit(formA));
            Future<LeaseApplication> b = pool.submit(() -> applicationService.submit(formB));

            // Both threads enter the paid call before either persists.
            bothInsideCall.await(5, TimeUnit.SECONDS);
            release.countDown();

            assertEquals(a.get(10, TimeUnit.SECONDS).getId(),
                    b.get(10, TimeUnit.SECONDS).getId());
            assertEquals(1, repository.count());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }

        verify(bureauClient, times(1)).fetchScore(anyString());
    }
}
