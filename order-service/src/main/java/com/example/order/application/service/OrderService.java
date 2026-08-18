package com.example.order.application.service;

import com.example.order.api.request.CompleteOrderDeliveryRequest;
import com.example.order.api.request.CreateOrderRequest;
import com.example.order.api.response.OrderResponse;
import com.example.order.domain.model.OrderEntity;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.client.WorkflowClient;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(OrderService.class.getName());

    private final OrderRepository orderRepository;
    private final WorkflowClient workflowClient;

    public OrderService(OrderRepository orderRepository, WorkflowClient workflowClient) {
        this.orderRepository = orderRepository;
        this.workflowClient = workflowClient;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        Span span = tracer.spanBuilder("order.create").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("order.id", request.orderId());
            span.setAttribute("customer.id", request.customerId());
            span.setAttribute("order.currency", request.currency());
            span.setAttribute("order.sku", request.sku());
            span.setAttribute("order.quantity", request.quantity());

            LOGGER.info("Creating order. orderId={}, customerId={}, correlationId={}",
                    request.orderId(), request.customerId(), request.correlationId());
            orderRepository.findById(request.orderId())
                    .ifPresent(existing -> {
                        LOGGER.warn("Order creation rejected because order already exists. orderId={}", existing.getOrderId());
                        throw new IllegalArgumentException("Order already exists: " + existing.getOrderId());
                    });

            String correlationId = normalizeCorrelationId(request.correlationId());
            span.setAttribute("correlation.id", correlationId);
            LOGGER.info("Persisting order before workflow start. orderId={}, correlationId={}",
                    request.orderId(), correlationId);
            OrderEntity order = new OrderEntity(
                    request.orderId(),
                    request.customerId(),
                    request.orderAmount(),
                    request.currency(),
                    request.sku(),
                    request.quantity(),
                    correlationId
            );
            orderRepository.saveAndFlush(order);

            LOGGER.info("Starting workflow for order. orderId={}, correlationId={}", request.orderId(), correlationId);
            WorkflowClient.WorkflowStartResponse workflow = workflowClient.startOrderWorkflow(request, correlationId);
            span.setAttribute("camunda.process_instance.id", workflow.processInstanceId());
            LOGGER.info("Workflow started for order. orderId={}, correlationId={}, processInstanceId={}, workflowStatus={}",
                    request.orderId(), correlationId, workflow.processInstanceId(), workflow.status());
            order.markProcessing(workflow.processInstanceId());
            orderRepository.saveAndFlush(order);
            LOGGER.info("Order marked processing. orderId={}, processInstanceId={}",
                    order.getOrderId(), workflow.processInstanceId());

            return OrderResponse.from(order);
        } catch (RuntimeException exception) {
            LOGGER.error("Order creation failed. orderId={}, message={}", request.orderId(), exception.getMessage(), exception);
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            throw exception;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    @Transactional
    public OrderResponse completeDelivery(
            String orderId,
            CompleteOrderDeliveryRequest request
    ) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getCorrelationId().equals(request.correlationId())) {
            throw new OrderConflictException(
                "Correlation ID does not match order: " + orderId
            );
        }

        if (request.deliveredAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Delivered time cannot be in the future"
            );
        }

        if (order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.FAILED) {
            throw new OrderConflictException(
                "Order cannot be delivered from status: "
                    + order.getStatus()
            );
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            if (request.deliveredAt().equals(order.getDeliveredAt())) {
                return OrderResponse.from(order);
            }

            throw new OrderConflictException(
                "Order delivery was already completed: " + orderId
            );
        }

        order.markDelivered(request.deliveredAt());

        return OrderResponse.from(orderRepository.save(order));
    }
}
