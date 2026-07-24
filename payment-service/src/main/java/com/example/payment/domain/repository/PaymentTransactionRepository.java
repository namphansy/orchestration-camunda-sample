package com.example.payment.domain.repository;

import com.example.payment.domain.model.PaymentTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
