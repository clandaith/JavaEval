package com.clicklease.app;

import com.clicklease.app.web.ApplicationForm;

import java.math.BigDecimal;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static ApplicationForm validForm() {
        return validForm(UUID.randomUUID().toString());
    }

    public static ApplicationForm validForm(String idempotencyKey) {
        ApplicationForm form = new ApplicationForm();
        form.setIdempotencyKey(idempotencyKey);
        form.setBusinessName("Acme Widgets LLC");
        form.setOwnerName("Jane Doe");
        form.setEmail("jane@acme.example");
        form.setTaxId("12-3456789");
        form.setEquipmentDescription("Forklift");
        form.setAmount(new BigDecimal("10000"));
        return form;
    }
}
