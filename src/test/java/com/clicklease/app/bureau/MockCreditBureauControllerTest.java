package com.clicklease.app.bureau;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockCreditBureauControllerTest {

    @Test
    void sameTaxIdAlwaysReturnsSameScore() {
        assertEquals(MockCreditBureauController.deterministicScore("12-3456789"),
                MockCreditBureauController.deterministicScore("12-3456789"));
    }

    @Test
    void scoreIsWithinVendorRange() {
        for (String taxId : new String[]{"00-0000000", "12-3456789", "99-9999999", "ab-cdefg"}) {
            int score = MockCreditBureauController.deterministicScore(taxId);
            assertTrue(score >= 300 && score <= 850, "score out of range: " + score);
        }
    }
}
