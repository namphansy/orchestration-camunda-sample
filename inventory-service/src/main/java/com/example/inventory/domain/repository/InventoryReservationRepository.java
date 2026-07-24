package com.example.inventory.domain.repository;

import com.example.inventory.domain.model.InventoryReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    Optional<InventoryReservation> findByIdempotencyKey(String idempotencyKey);
}
