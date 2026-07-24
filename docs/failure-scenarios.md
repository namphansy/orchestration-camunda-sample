# Failure Scenarios

## Insufficient Stock

`inventory-service` seeds `SKU-DEFAULT` with quantity `100`. Request a larger quantity to receive HTTP `409` from Inventory:

```bash
curl -X POST http://localhost:8082/api/inventory/reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: insufficient-stock-demo" \
  -d '{
    "orderId": "order-insufficient-1",
    "sku": "SKU-DEFAULT",
    "quantity": 1000,
    "correlationId": "correlation-insufficient-1"
  }'
```

When the same shortage happens through `workflow-service`, the `Reserve Inventory` delegate throws BPMN error `INSUFFICIENT_STOCK`, and the process follows `Reject Order`.

Payment decline, payment timeout, technical payment failure, shipment failure, notification failure, RabbitMQ poison messages, and worker crashes are still future-phase scenarios.
