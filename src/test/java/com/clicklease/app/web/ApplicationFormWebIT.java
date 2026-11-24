package com.clicklease.app.web;

import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.repository.LeaseApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: invalid form submissions re-render inline validation
 * errors and never reach the bureau or the database. Also covers EIN masking
 * on rendered pages, the status filter, and retry-button visibility.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "bureau.failure-rate=0.0",
        "bureau.min-delay-ms=1",
        "bureau.max-delay-ms=5"
})
class ApplicationFormWebIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LeaseApplicationRepository repository;

    @Test
    void invalidSubmissionShowsInlineErrorsAndSavesNothing() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("idempotencyKey", "bad-form-key");
        form.add("businessName", "");
        form.add("ownerName", "");
        form.add("email", "not-an-email");
        form.add("taxId", "BAD-EIN");
        form.add("equipmentDescription", "");
        form.add("amount", "999999");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        long rowsBefore = repository.count();
        String body = restTemplate.postForObject("/applications",
                new HttpEntity<>(form, headers), String.class);

        assertEquals(rowsBefore, repository.count());
        assertTrue(body.contains("Business name is required")
                || body.contains("required"), "expected inline validation errors");
        assertTrue(body.contains("XX-XXXXXXX"), "expected EIN format error");
    }

    @Test
    void taxIdNeverAppearsInFullOnListPage() {
        String body = restTemplate.getForObject("/applications", String.class);
        assertFalse(body.contains("12-3456789"));
    }

    @Test
    void detailPageMasksTaxIdAndShowsRetryForPending() {
        LeaseApplication pending = repository.save(new LeaseApplication(
                UUID.randomUUID().toString(), "Masked Biz", "Ann", "ann@x.io",
                "12-3456789", "Lathe", new BigDecimal("5000")));

        String body = restTemplate.getForObject("/applications/" + pending.getId(), String.class);

        assertFalse(body.contains("12-3456789"), "full EIN must never render");
        assertTrue(body.contains("**-***6789"), "expected masked EIN");
        assertTrue(body.contains("Retry decision"), "PENDING detail must offer retry");
    }

    @Test
    void formRendersFreshIdempotencyKey() {
        String first = restTemplate.getForObject("/", String.class);
        String second = restTemplate.getForObject("/", String.class);

        java.util.regex.Pattern keyPattern =
                java.util.regex.Pattern.compile("name=\"idempotencyKey\" value=\"([0-9a-f-]{36})\"");
        var m1 = keyPattern.matcher(first);
        var m2 = keyPattern.matcher(second);
        assertTrue(m1.find(), "form must carry a hidden idempotency key");
        assertTrue(m2.find(), "form must carry a hidden idempotency key");
        assertFalse(m1.group(1).equals(m2.group(1)),
                "each form render should mint a new key");
    }

    @Test
    void retryOnUnknownApplicationReturns404() {
        var response = restTemplate.postForEntity("/applications/999999/retry", null, String.class);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void unknownApplicationIdReturns404() {
        var response = restTemplate.getForEntity("/applications/999999", String.class);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listIsOrderedNewestFirst() {
        repository.save(new LeaseApplication(UUID.randomUUID().toString(),
                "Older Biz", "A", "a@x.io", "11-1111111", "Eq", new BigDecimal("5000")));
        try {
            Thread.sleep(10); // timestamps are ms-precision — keep ordering deterministic
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        repository.save(new LeaseApplication(UUID.randomUUID().toString(),
                "Newer Biz", "B", "b@x.io", "22-2222222", "Eq", new BigDecimal("5000")));

        String body = restTemplate.getForObject("/applications", String.class);

        assertTrue(body.indexOf("Newer Biz") < body.indexOf("Older Biz"),
                "newest application must be listed first");
    }

    @Test
    void statusFilterShowsOnlyMatchingApplications() {
        repository.save(new LeaseApplication(UUID.randomUUID().toString(),
                "Pending Biz", "A", "a@x.io", "11-1111111", "Eq", new BigDecimal("5000")));
        LeaseApplication approved = repository.save(new LeaseApplication(
                UUID.randomUUID().toString(), "Approved Biz", "B", "b@x.io",
                "22-2222222", "Eq", new BigDecimal("5000")));
        approved.setStatus(ApplicationStatus.APPROVED);
        repository.save(approved);

        String body = restTemplate.getForObject("/applications?status=PENDING", String.class);

        assertTrue(body.contains("Pending Biz"));
        assertFalse(body.contains("Approved Biz"));
    }
}
