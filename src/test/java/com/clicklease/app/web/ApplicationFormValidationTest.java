package com.clicklease.app.web;

import com.clicklease.app.TestFixtures;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for every field rule in the PRD's form table — exercised
 * through the Bean Validation annotations on {@link ApplicationForm}.
 */
class ApplicationFormValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> violatedProperties(ApplicationForm form) {
        return validator.validate(form).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void fullyValidFormHasNoViolations() {
        assertEquals(Set.of(), violatedProperties(TestFixtures.validForm()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void requiredFieldsRejectBlank(String blank) {
        for (String field : new String[]{"businessName", "ownerName", "equipmentDescription", "email", "taxId"}) {
            ApplicationForm form = TestFixtures.validForm();
            switch (field) {
                case "businessName" -> form.setBusinessName(blank);
                case "ownerName" -> form.setOwnerName(blank);
                case "equipmentDescription" -> form.setEquipmentDescription(blank);
                case "email" -> form.setEmail(blank);
                case "taxId" -> form.setTaxId(blank);
                default -> throw new IllegalArgumentException(field);
            }
            assertTrue(violatedProperties(form).contains(field),
                    field + " must reject '" + blank + "'");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-at.com", "@nodomain.com", "spaces in@email.com"})
    void invalidEmailRejected(String email) {
        ApplicationForm form = TestFixtures.validForm();
        form.setEmail(email);
        assertTrue(violatedProperties(form).contains("email"), "email should reject " + email);
    }

    @Test
    void plausibleEmailAccepted() {
        ApplicationForm form = TestFixtures.validForm();
        form.setEmail("owner@company.com");
        assertTrue(violatedProperties(form).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456789",     // no dash
            "1-2345678",     // one-digit prefix
            "123-456789",    // three-digit prefix
            "12-345678",     // six-digit suffix
            "12-34567890",   // eight-digit suffix
            "ab-cdefghi",    // letters
            "12_3456789"     // wrong separator
    })
    void malformedEinRejected(String taxId) {
        ApplicationForm form = TestFixtures.validForm();
        form.setTaxId(taxId);
        assertTrue(violatedProperties(form).contains("taxId"), "EIN should reject " + taxId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12-3456789", "99-9999999", "00-0000000"})
    void wellFormedEinAccepted(String taxId) {
        ApplicationForm form = TestFixtures.validForm();
        form.setTaxId(taxId);
        assertTrue(violatedProperties(form).isEmpty(), "EIN should accept " + taxId);
    }

    @Test
    void amountMustBePresent() {
        ApplicationForm form = TestFixtures.validForm();
        form.setAmount(null);
        assertTrue(violatedProperties(form).contains("amount"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"499.99", "0", "-100", "50000.01", "99999"})
    void amountOutOfRangeRejected(String amount) {
        ApplicationForm form = TestFixtures.validForm();
        form.setAmount(new BigDecimal(amount));
        assertTrue(violatedProperties(form).contains("amount"), "amount should reject " + amount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"500", "500.00", "25000", "50000"})
    void amountWithinRangeAccepted(String amount) {
        ApplicationForm form = TestFixtures.validForm();
        form.setAmount(new BigDecimal(amount));
        assertTrue(violatedProperties(form).isEmpty(), "amount should accept " + amount);
    }
}
