package com.example.workflow.domain.repository;

import com.example.workflow.domain.model.FraudReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudReviewRepository extends JpaRepository<FraudReviewEntity, String> {
    Optional<FraudReviewEntity> findByOrderId(String orderId);
    Optional<FraudReviewEntity> findByProcessInstanceId(String processInstanceId);
}
