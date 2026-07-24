# API Contracts

Phase 2 exposes Actuator endpoints, the workflow API, the order API, and the inventory reservation API.

## Current Endpoints

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
GET /prometheus
POST /api/workflows/orders
GET /api/workflows/orders/{businessKey}
POST /api/orders
GET /api/orders/{orderId}
POST /api/inventory/reservations
GET /api/inventory/reservations/{reservationId}
```

## Start Workflow Request

```json
{
  "orderId": "order-1001",
  "correlationId": "correlation-1001"
}
```

## Start Workflow Response

```json
{
  "processInstanceId": "camunda-process-instance-id",
  "businessKey": "order-1001",
  "correlationId": "correlation-1001",
  "status": "COMPLETED"
}
```

## Create Order Request

```json
{
  "orderId": "order-2001",
  "customerId": "customer-42",
  "orderAmount": 120.50,
  "currency": "USD",
  "sku": "SKU-DEFAULT",
  "quantity": 1,
  "correlationId": "correlation-2001"
}
```

Creating an order persists the order in `order-service` and starts `order-processing` through `workflow-service`.

The workflow start contract intentionally contains only workflow identity and tracing data. Business order details remain owned by `order-service`.

## Inventory Reservation Request

Required header:

```text
Idempotency-Key: inventory-reservation-order-2001
```

```json
{
  "orderId": "order-2001",
  "sku": "SKU-DEFAULT",
  "quantity": 1,
  "correlationId": "correlation-2001"
}
```

Duplicate successful requests with the same `Idempotency-Key` return the original reservation and do not decrement stock again.

Insufficient stock returns HTTP `409`:

```json
{
  "timestamp": "2026-07-17T10:00:00Z",
  "service": "inventory-service",
  "correlationId": "",
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for SKU SKU-DEFAULT: requested 1000, available 100",
  "details": {}
}
```
