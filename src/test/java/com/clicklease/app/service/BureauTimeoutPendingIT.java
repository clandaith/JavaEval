package com.clicklease.app.service;

import com.clicklease.app.TestFixtures;
import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Required test #2 (timeout path): a bureau response slower than the client
 * timeout also lands the application in Pending — the UI never hangs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "bureau.failure-rate=0.0",
        "bureau.min-delay-ms=2500",
        "bureau.max-delay-ms=2600",
        "bureau.timeout-ms=300"
})
class BureauTimeoutPendingIT {

    @Autowired
    private ApplicationService applicationService;

    @Test
    void bureauTimeoutSavesApplicationAsPending() {
        LeaseApplication application = applicationService.submit(TestFixtures.validForm());
        assertEquals(ApplicationStatus.PENDING, application.getStatus());
    }
}
