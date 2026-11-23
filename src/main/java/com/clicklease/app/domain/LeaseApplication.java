package com.clicklease.app.domain;

import com.clicklease.app.util.TaxIdMasker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lease_applications", indexes = {
        @Index(name = "idx_lease_app_idempotency_key", columnList = "idempotencyKey", unique = true)
})
public class LeaseApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String taxId;

    @Column(nullable = false)
    private String equipmentDescription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private Integer creditScore;

    private String decisionReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected LeaseApplication() {
    }

    public LeaseApplication(String idempotencyKey, String businessName, String ownerName,
                            String email, String taxId, String equipmentDescription,
                            BigDecimal amount) {
        this.idempotencyKey = idempotencyKey;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.email = email;
        this.taxId = taxId;
        this.equipmentDescription = equipmentDescription;
        this.amount = amount;
        this.status = ApplicationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getEmail() {
        return email;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getMaskedTaxId() {
        return TaxIdMasker.mask(taxId);
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
