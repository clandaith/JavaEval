package com.clicklease.app.repository;

import com.clicklease.app.domain.ApplicationStatus;
import com.clicklease.app.domain.LeaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaseApplicationRepository extends JpaRepository<LeaseApplication, Long> {

    Optional<LeaseApplication> findByIdempotencyKey(String idempotencyKey);

    List<LeaseApplication> findAllByOrderByCreatedAtDesc();

    List<LeaseApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
}
