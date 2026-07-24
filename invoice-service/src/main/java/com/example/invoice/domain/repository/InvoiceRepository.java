package com.example.invoice.domain.repository;

import com.example.invoice.domain.model.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
}
