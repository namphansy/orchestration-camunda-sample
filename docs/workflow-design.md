# Workflow Design

The default executable Camunda 7 BPMN process is:

```text
order-processing.bpmn
```

Phase 9 adds a parallel External Task implementation branch:

```text
order-processing-external.bpmn
payment-subprocess-external.bpmn
shipping-subprocess-external.bpmn
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

In the external-task branch, inventory reservation, payment charging, and shipment creation are modeled as Camunda External Tasks:

```text
reserve-inventory
charge-payment
create-shipment
```

`external-task-worker` subscribes to these topics over `/engine-rest`. Stopping the worker does not stop the process engine; locked tasks remain in Camunda and become fetchable again after the lock duration expires.

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

External worker technical failures call `handleFailure` with a decremented retry count, retry timeout, error message, and stack details. Those failure fields are stored on the external task and are visible from Cockpit. Business failures use `handleBpmnError` with the existing error codes: `INSUFFICIENT_STOCK`, `PAYMENT_DECLINED`, and `SHIPMENT_FAILED`.

Payment confirmation timeout is modeled as a boundary timer on `Wait for Payment Confirmation`. The timeout path sets `paymentStatus=TIMED_OUT`, records `failureReason=Payment confirmation timed out`, rejects the order, and ends on `Order Rejected`.

## Retry Strategy

No retry cycles are configured in Phase 2. Retry and incident behavior starts in Phase 3.

## Compensation Strategy

Saga compensation is not implemented in Phase 2.

## Versioning Notes

The default process definition key remains `order-processing`. Phase 9 adds `order-processing-external` as a second implementation branch and preserves existing variable names for compatibility.
