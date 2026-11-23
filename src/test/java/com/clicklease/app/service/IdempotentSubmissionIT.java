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
        var form = TestFixtures.validForm("idempotency-key-abc-123");

        LeaseApplication first = applicationService.submit(form);
        LeaseApplication second = applicationService.submit(form); // re-submit, same key

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getStatus(), second.getStatus());
        assertEquals(1, registry.getCallCount(), "bureau must not be billed twice");
        assertEquals(1, repository.count(), "only one row may be persisted");
    }
}
