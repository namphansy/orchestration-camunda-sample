# Workflow Design

Phase 2 extends the executable Camunda 7 BPMN process:

```text
order-processing.bpmn
```

## Activities

```text
Start Order
  -> Validate Order
  -> Reserve Inventory
  -> Charge Payment
  -> Complete Order
  -> Order Completed
```

`Reserve Inventory` first queries `order-service` by `orderId` to get the current order details needed for reservation, then calls `inventory-service` through a thin REST client. The other service tasks remain learning placeholders until their owning phases.

The inventory business-error path is:

```text
Reserve Inventory
  -> Insufficient Stock boundary error
  -> Reject Order
  -> Order Rejected
```

## Process Variables

Current variables:

```text
orderId
businessKey
correlationId
inventoryReservationId
orderStatus
inventoryStatus
paymentStatus
failureReason
```

The `orderId` is used as the Camunda business key. Business payload such as customer, amount, currency, SKU, and quantity stays in `order-service` and is fetched by delegates when needed.

## Message Correlation Strategy

Message correlation is not implemented in Phase 2. Later phases will correlate messages by business key and correlation ID.

## Error Handling Strategy

Inventory shortage is modeled as BPMN error code `INSUFFICIENT_STOCK`. The workflow catches it on `Reserve Inventory`, sets `inventoryStatus=INSUFFICIENT_STOCK`, sets `orderStatus=REJECTED`, and ends on `Order Rejected`.

Unexpected inventory REST failures still propagate as technical exceptions so Camunda can treat them as failed jobs in later retry phases.

## Retry Strategy

No retry cycles are configured in Phase 2. Retry and incident behavior starts in Phase 3.

## Compensation Strategy

Saga compensation is not implemented in Phase 2.

## Versioning Notes

The process definition key remains `order-processing`. Phase 2 adds variables and a boundary error; existing Phase 1 variable names are preserved.
