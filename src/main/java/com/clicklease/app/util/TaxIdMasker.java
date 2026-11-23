package com.clicklease.app.util;

public final class TaxIdMasker {

    private TaxIdMasker() {
    }

    public static String mask(String taxId) {
        if (taxId == null) {
            return "**-***----";
        }
        String digits = taxId.replaceAll("\\D", "");
        String last4 = digits.length() >= 4
                ? digits.substring(digits.length() - 4)
                : "----";
        return "**-***" + last4;
    }
}
