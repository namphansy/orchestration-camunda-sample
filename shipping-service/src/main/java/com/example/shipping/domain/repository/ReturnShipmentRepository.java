package com.example.shipping.domain.repository;

import com.example.shipping.domain.model.ReturnShipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnShipmentRepository
        extends JpaRepository<ReturnShipment, String> {

    Optional<ReturnShipment> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<ReturnShipment> findByReturnId(
            String returnId
    );
}