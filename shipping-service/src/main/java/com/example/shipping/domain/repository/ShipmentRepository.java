package com.example.shipping.domain.repository;

import com.example.shipping.domain.model.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    Optional<Shipment> findByIdempotencyKey(String idempotencyKey);
}
