package com.example.shipping.application.service;

import com.example.shipping.api.request.CreateReturnShipmentRequest;
import com.example.shipping.api.response.ReturnShipmentResponse;
import com.example.shipping.domain.model.ReturnShipment;
import com.example.shipping.domain.model.ReturnShipmentStatus;
import com.example.shipping.domain.repository.ReturnShipmentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnShipmentService {

    private final ReturnShipmentRepository returnShipmentRepository;

    public ReturnShipmentService(
            ReturnShipmentRepository returnShipmentRepository
    ) {
        this.returnShipmentRepository = returnShipmentRepository;
    }

    @Transactional
    public ReturnShipmentResponse create(
            CreateReturnShipmentRequest request,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        ReturnShipment existingByKey = returnShipmentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingByKey != null) {
            validateSameRequest(existingByKey, request);
            return ReturnShipmentResponse.from(existingByKey);
        }

        ReturnShipment existingByReturnId = returnShipmentRepository
                .findByReturnId(request.returnId())
                .orElse(null);

        if (existingByReturnId != null) {
            throw new IllegalStateException(
                    "Return shipment already exists for return: "
                            + request.returnId()
            );
        }

        ReturnShipment returnShipment = new ReturnShipment(
                UUID.randomUUID().toString(),
                request.returnId(),
                request.orderId(),
                request.customerId(),
                ReturnShipmentStatus.CREATED,
                idempotencyKey,
                request.correlationId()
        );

        return ReturnShipmentResponse.from(
                returnShipmentRepository.save(returnShipment)
        );
    }

    @Transactional(readOnly = true)
    public ReturnShipmentResponse get(String returnShipmentId) {
        return returnShipmentRepository.findById(returnShipmentId)
                .map(ReturnShipmentResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Return shipment not found: " + returnShipmentId
                ));
    }

    private void validateSameRequest(
            ReturnShipment existing,
            CreateReturnShipmentRequest request
    ) {
        boolean sameRequest =
                existing.getReturnId().equals(request.returnId())
                && existing.getOrderId().equals(request.orderId())
                && existing.getCustomerId().equals(request.customerId())
                && existing.getCorrelationId()
                        .equals(request.correlationId());

        if (!sameRequest) {
            throw new IllegalStateException(
                    "Idempotency-Key was already used for another request"
            );
        }
    }
}