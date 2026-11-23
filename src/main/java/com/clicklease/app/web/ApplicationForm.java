package com.clicklease.app.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class ApplicationForm {

    private String idempotencyKey;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Owner full name is required")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Tax ID (EIN) is required")
    @Pattern(regexp = "\\d{2}-\\d{7}", message = "Must be in format XX-XXXXXXX")
    private String taxId;

    @NotBlank(message = "Equipment description is required")
    private String equipmentDescription;

    @NotNull(message = "Lease amount is required")
    @DecimalMin(value = "500", message = "Amount must be at least $500")
    @DecimalMax(value = "50000", message = "Amount must be at most $50,000")
    private BigDecimal amount;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public void setEquipmentDescription(String equipmentDescription) {
        this.equipmentDescription = equipmentDescription;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
