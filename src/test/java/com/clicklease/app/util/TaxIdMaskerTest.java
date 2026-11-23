package com.clicklease.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaxIdMaskerTest {

    @Test
    void masksAllButLastFourDigits() {
        assertEquals("**-***6789", TaxIdMasker.mask("12-3456789"));
        assertFalse(TaxIdMasker.mask("12-3456789").contains("12-34"));
    }

    @Test
    void masksDigitsWithoutDashes() {
        assertEquals("**-***6789", TaxIdMasker.mask("123456789"));
    }

    @Test
    void handlesNullAndShortInput() {
        assertEquals("**-***----", TaxIdMasker.mask(null));
        assertEquals("**-***----", TaxIdMasker.mask("12"));
    }
}
