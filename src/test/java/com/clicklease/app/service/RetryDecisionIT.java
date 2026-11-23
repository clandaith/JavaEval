package com.clicklease.app.service;

import com.clicklease.app.TestFixtures;
import com.clicklease.app.bureau.BureauClient;
import com.clicklease.app.bureau.BureauUnavailableException;
import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Retry-decision behavior: a PENDING application can call the bureau again and
 * be updated, while a decided application must never trigger a new paid call.
 * BureauClient is mocked so the pending->decided transition is deterministic.
 */
@SpringBootTest
class RetryDecisionIT {

    @Autowired
    private ApplicationService applicationService;

    @MockitoBean
    private BureauClient bureauClient;

    @Test
    void retryOnPendingCallsBureauAgainAndUpdatesRecord() {
        when(bureauClient.fetchScore(anyString()))
                .thenThrow(new BureauUnavailableException("vendor down", null))
                .thenReturn(800);

        LeaseApplication application = applicationService.submit(TestFixtures.validForm());
        assertEquals(ApplicationStatus.PENDING, application.getStatus());

        LeaseApplication retried = applicationService.retryDecision(application.getId());

        assertEquals(ApplicationStatus.APPROVED, retried.getStatus());
        assertEquals(800, retried.getCreditScore());
        verify(bureauClient, times(2)).fetchScore(anyString());
    }

    @Test
    void retryOnDecidedApplicationDoesNotCallBureauAgain() {
        when(bureauClient.fetchScore(anyString())).thenReturn(700);

        LeaseApplication application = applicationService.submit(TestFixtures.validForm());
        assertEquals(ApplicationStatus.APPROVED, application.getStatus());

        LeaseApplication retried = applicationService.retryDecision(application.getId());

        assertEquals(ApplicationStatus.APPROVED, retried.getStatus());
        // Still exactly one paid call — the guard blocks re-billing the vendor.
        verify(bureauClient, times(1)).fetchScore(anyString());
        Mockito.verifyNoMoreInteractions(bureauClient);
    }
}
