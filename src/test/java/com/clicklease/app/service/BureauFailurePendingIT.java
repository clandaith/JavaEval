package com.clicklease.app.service;

import com.clicklease.app.TestFixtures;
import com.clicklease.app.bureau.BureauCallRegistry;
import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.repository.LeaseApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Required test #2 (failure path): when the bureau fails, the application is
 * saved as Pending instead of erroring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "bureau.failure-rate=1.0",
        "bureau.min-delay-ms=1",
        "bureau.max-delay-ms=5"
})
class BureauFailurePendingIT {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private LeaseApplicationRepository repository;

    @Autowired
    private BureauCallRegistry registry;

    @Test
    void bureauFailureSavesApplicationAsPending() {
        var form = TestFixtures.validForm();

        LeaseApplication application = applicationService.submit(form);

        assertEquals(ApplicationStatus.PENDING, application.getStatus());
        assertNull(application.getCreditScore());
        assertNotNull(application.getDecisionReason());
        // The record is persisted — a retry is possible later.
        assertNotNull(application.getId());
        assertTrue(repository.findByIdempotencyKey(form.getIdempotencyKey()).isPresent());
        // The paid call was attempted exactly once.
        assertEquals(1, registry.getCallCount());
    }
}
