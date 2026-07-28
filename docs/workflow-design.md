# Workflow Design

Phase 6 extends the executable Camunda 7 BPMN process:

```text
order-processing.bpmn
```

## Activities

```text
Start Order
  -> Validate Order
  -> Reserve Inventory
  -> Charge Payment
  -> Wait for Payment Confirmation
  -> Complete Order
  -> Order Completed
```

`Reserve Inventory` first queries `order-service` by `orderId` to get the current order details needed for reservation, then calls `inventory-service` through a thin REST client. `Charge Payment` initiates the payment and stores `paymentStatus=PENDING`; the workflow resumes only after the `PaymentConfirmationReceived` message is correlated.

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
paymentConfirmed
paymentTransactionId
failureReason
```

The `orderId` is used as the Camunda business key. Business payload such as customer, amount, currency, SKU, and quantity stays in `order-service` and is fetched by delegates when needed.

## Message Correlation Strategy

Payment confirmations are correlated by Camunda business key (`orderId`) and the `correlationId` process variable. The REST endpoint is:

```text
POST /api/workflows/orders/{businessKey}/payment-confirmations
```

Unknown, mismatched, duplicate, or late confirmations return `IGNORED` and do not move the process.

## Error Handling Strategy

Inventory shortage is modeled as BPMN error code `INSUFFICIENT_STOCK`. The workflow catches it on `Reserve Inventory`, sets `inventoryStatus=INSUFFICIENT_STOCK`, sets `orderStatus=REJECTED`, and ends on `Order Rejected`.

Unexpected inventory REST failures still propagate as technical exceptions so Camunda can treat them as failed jobs in later retry phases.

Payment confirmation timeout is modeled as a boundary timer on `Wait for Payment Confirmation`. The timeout path sets `paymentStatus=TIMED_OUT`, records `failureReason=Payment confirmation timed out`, rejects the order, and ends on `Order Rejected`.

## Retry Strategy

No retry cycles are configured in Phase 2. Retry and incident behavior starts in Phase 3.

## Compensation Strategy

Saga compensation is not implemented in Phase 2.

## Versioning Notes

The process definition key remains `order-processing`. Phase 6 preserves existing variable names and adds payment confirmation variables for asynchronous message handling.
