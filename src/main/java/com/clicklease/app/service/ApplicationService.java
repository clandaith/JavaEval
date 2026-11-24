package com.clicklease.app.service;

import com.clicklease.app.bureau.BureauClient;
import com.clicklease.app.bureau.BureauUnavailableException;
import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import com.clicklease.app.repository.LeaseApplicationRepository;
import com.clicklease.app.util.TaxIdMasker;
import com.clicklease.app.web.ApplicationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    /**
     * Striped locks keyed on idempotency key: a concurrent resubmit of the
     * same key waits for the winner instead of double-billing the bureau.
     * Bounded to a fixed stripe count so no per-key state accumulates.
     */
    private static final int SUBMIT_LOCK_STRIPES = 64;
    private final Object[] submitLocks = new Object[SUBMIT_LOCK_STRIPES];

    private final LeaseApplicationRepository repository;
    private final BureauClient bureauClient;
    private final DecisionService decisionService;

    public ApplicationService(LeaseApplicationRepository repository,
                              BureauClient bureauClient,
                              DecisionService decisionService) {
        this.repository = repository;
        this.bureauClient = bureauClient;
        this.decisionService = decisionService;
        Arrays.setAll(submitLocks, i -> new Object());
    }

    /**
     * Idempotent submit: the same idempotency key can never trigger a second
     * paid bureau call — repeats return the already-saved application.
     */
    public LeaseApplication submit(ApplicationForm form) {
        String key = (form.getIdempotencyKey() == null || form.getIdempotencyKey().isBlank())
                ? UUID.randomUUID().toString()
                : form.getIdempotencyKey();

        synchronized (lockFor(key)) {
            var existing = repository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                logger.info("DEBUG MSG: Duplicate submission for key={} — returning existing "
                        + "application id={} without calling the bureau", key, existing.get().getId());
                return existing.get();
            }

            LeaseApplication application = new LeaseApplication(
                    key, form.getBusinessName(), form.getOwnerName(), form.getEmail(),
                    form.getTaxId(), form.getEquipmentDescription(), form.getAmount());
            applyDecision(application);
            return saveIdempotently(application, key);
        }
    }

    private Object lockFor(String key) {
        return submitLocks[Math.floorMod(key.hashCode(), SUBMIT_LOCK_STRIPES)];
    }

    /**
     * Re-runs the bureau call for a PENDING application. Each retry is a new,
     * deliberately paid bureau call.
     */
    public LeaseApplication retryDecision(Long id) {
        LeaseApplication application = repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        if (application.getStatus() != ApplicationStatus.PENDING) {
            logger.info("DEBUG MSG: Retry requested for application id={} with status={} — "
                    + "only PENDING applications may be retried", id, application.getStatus());
            return application;
        }
        logger.info("DEBUG MSG: Retrying decision for application id={}", id);
        applyDecision(application);
        return repository.save(application);
    }

    public List<LeaseApplication> listApplications(ApplicationStatus statusFilter) {
        if (statusFilter == null) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByStatusOrderByCreatedAtDesc(statusFilter);
    }

    public LeaseApplication getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private void applyDecision(LeaseApplication application) {
        DecisionService.Decision decision;
        try {
            int score = bureauClient.fetchScore(application.getTaxId());
            application.setCreditScore(score);
            decision = decisionService.decide(score, application.getAmount());
        } catch (BureauUnavailableException e) {
            logger.info("DEBUG MSG: Bureau unavailable for application taxId={} — "
                    + "marking PENDING", TaxIdMasker.mask(application.getTaxId()));
            decision = decisionService.pending(e.getMessage());
        }
        application.setStatus(decision.status());
        application.setDecisionReason(decision.reason());
    }

    private LeaseApplication saveIdempotently(LeaseApplication application, String key) {
        try {
            LeaseApplication saved = repository.save(application);
            logger.info("DEBUG MSG: Saved application id={} status={} for taxId={}",
                    saved.getId(), saved.getStatus(), TaxIdMasker.mask(saved.getTaxId()));
            return saved;
        } catch (DataAccessException e) {
            // Last line of defense: a unique-constraint violation means a duplicate
            // won the insert race (SQLite reports this as JpaSystemException, not
            // DataIntegrityViolationException) — return the winner's row.
            logger.info("DEBUG MSG: Concurrent duplicate for key={} — returning existing row", key);
            return repository.findByIdempotencyKey(key).orElseThrow(() -> e);
        }
    }
}
