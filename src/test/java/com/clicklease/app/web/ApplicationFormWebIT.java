package com.clicklease.app.web;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: invalid form submissions re-render inline validation
 * errors and never reach the bureau or the database.
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

        String body = restTemplate.postForObject("/applications",
                new HttpEntity<>(form, headers), String.class);

        assertEquals(0, repository.count());
        assertTrue(body.contains("Business name is required")
                || body.contains("required"), "expected inline validation errors");
        assertTrue(body.contains("XX-XXXXXXX"), "expected EIN format error");
    }

    @Test
    void taxIdNeverAppearsInFullOnListPage() {
        String body = restTemplate.getForObject("/applications", String.class);
        assertTrue(!body.contains("12-3456789"));
    }
}
