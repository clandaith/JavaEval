package com.clicklease.app.service;

import com.clicklease.app.TestFixtures;
import com.clicklease.app.bureau.BureauCallRegistry;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.repository.LeaseApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Required test #3: submitting the same application twice (double click /
 * refresh) calls the paid bureau endpoint only once.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "bureau.failure-rate=0.0",
        "bureau.min-delay-ms=1",
        "bureau.max-delay-ms=5"
})
class IdempotentSubmissionIT {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private LeaseApplicationRepository repository;

    @Autowired
    private BureauCallRegistry registry;

    @Test
    void duplicateSubmissionCallsBureauOnlyOnce() {
        long callsBefore = registry.getCallCount();
        long rowsBefore = repository.count();
        var form = TestFixtures.validForm("idempotency-key-abc-123");

        LeaseApplication first = applicationService.submit(form);
        LeaseApplication second = applicationService.submit(form); // re-submit, same key

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getStatus(), second.getStatus());
        assertEquals(callsBefore + 1, registry.getCallCount(), "bureau must not be billed twice");
        assertEquals(rowsBefore + 1, repository.count(), "only one row may be persisted");
    }

    @Test
    void sameKeyWithDifferentDataStillDeduplicates() {
        long callsBefore = registry.getCallCount();
        var first = TestFixtures.validForm("shared-key");
        var second = TestFixtures.validForm("shared-key");
        second.setBusinessName("Totally Different Corp");
        second.setAmount(new java.math.BigDecimal("49000"));

        LeaseApplication a = applicationService.submit(first);
        LeaseApplication b = applicationService.submit(second);

        assertEquals(a.getId(), b.getId());
        assertEquals("Acme Widgets LLC", b.getBusinessName(),
                "first write wins — dedupe is on the key, not the content");
        assertEquals(callsBefore + 1, registry.getCallCount());
    }

    @Test
    void differentKeysAreSeparateBillableSubmissions() {
        long callsBefore = registry.getCallCount();

        applicationService.submit(TestFixtures.validForm("key-one"));
        applicationService.submit(TestFixtures.validForm("key-two"));

        assertEquals(callsBefore + 2, registry.getCallCount());
        assertTrue(repository.findByIdempotencyKey("key-one").isPresent());
        assertTrue(repository.findByIdempotencyKey("key-two").isPresent());
    }
}
