package com.example.order.application.service;

import com.example.order.api.request.CreateOrderRequest;
import com.example.order.api.response.OrderResponse;
import com.example.order.domain.model.OrderEntity;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.client.WorkflowClient;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

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

            orderRepository.findById(request.orderId())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Order already exists: " + existing.getOrderId());
                    });

            String correlationId = normalizeCorrelationId(request.correlationId());
            span.setAttribute("correlation.id", correlationId);
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

            WorkflowClient.WorkflowStartResponse workflow = workflowClient.startOrderWorkflow(request, correlationId);
            span.setAttribute("camunda.process_instance.id", workflow.processInstanceId());
            order.markProcessing(workflow.processInstanceId());

            return OrderResponse.from(order);
        } catch (RuntimeException exception) {
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
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
