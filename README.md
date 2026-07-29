# Camunda Order Orchestration

Production-oriented learning project for demonstrating how Camunda 7 can orchestrate an order-processing workflow across Spring Boot microservices.

## Phase 9 Status

This repository currently contains the bootstrap skeleton, a Camunda 7 workflow, and real Order, Inventory, Payment, Invoice, and Notification service integrations.

Implemented:

- Embedded Camunda 7 process engine in `workflow-service`.
- Camunda web applications for local Cockpit and Tasklist access.
- `order-processing.bpmn` with inventory reservation, payment charging, shipping, a payment-result exclusive gateway, DMN-driven approval routing, manager/director user tasks, invoice/notification work, modeled business-error paths, compensation, and Camunda retry configuration.
- Parallel External Task workflow definitions: `order-processing-external`, `payment-subprocess-external`, and `shipping-subprocess-external`.
- `external-task-worker`, an independently deployable Spring Boot 3.3.7 worker that subscribes to inventory, payment, and shipment topics over Camunda REST.
- `order-approval.dmn` decision table that maps order amount to `NONE`, `MANAGER`, or `DIRECTOR` approval levels.
- REST API to start and query an order workflow, plus list, claim, and complete approval tasks.
- `order-service` persistence and order creation/query API.
- `inventory-service` stock persistence, reservation API, and idempotent duplicate handling.
- `payment-service` transaction persistence, charge API, payment decline simulation, technical failure simulation, configurable processing delay, and idempotent duplicate handling.
- `invoice-service` invoice persistence, generation/query API, and idempotent duplicate handling.
- `notification-service` publishing API skeleton with configurable failure simulation.
- REST integration from `workflow-service` to `order-service`, `inventory-service`, `payment-service`, `invoice-service`, and `notification-service`.
- Tests proving process and DMN deployment, process completion, insufficient-stock rejection, payment decline rejection, payment technical retries, incident generation after retry exhaustion, DMN approval routing, approval/rejection paths, approval task REST APIs, notification failure isolation, external task deployment/locking/failure details, worker handler completion/BPMN-error/retry behavior, REST workflow start, order creation, inventory idempotency, payment idempotency, invoice idempotency, notification publishing, and Spring context startup.

Not implemented yet:

- RabbitMQ integration.

## Architecture Overview

The repository is a Maven multi-module project with one module per service:

| Module | Responsibility |
| --- | --- |
| `workflow-service` | Camunda 7 process engine host and workflow API. |
| `external-task-worker` | Independently deployable Camunda External Task worker for inventory, payment, and shipment topics. |
| `order-service` | Owns persisted orders and starts order workflows. |
| `inventory-service` | Owns stock and idempotent inventory reservations. |
| `payment-service` | Owns idempotent payment charge transactions and payment failure simulation. |
| `shipping-service` | Future shipment creation and cancellation API. |
| `invoice-service` | Owns idempotent invoice generation. |
| `notification-service` | Provides a notification publishing API skeleton. |
| `integration-tests` | Future cross-service and Testcontainers scenarios. |

`order-service` starts workflows. The default `order-processing` path uses embedded JavaDelegates in `workflow-service`. The Phase 9 external-task branch uses `order-processing-external` and leaves inventory reservation, payment charging, and shipment creation for `external-task-worker` to fetch, lock, complete, retry, or fail over Camunda REST. Paid orders pass through an exclusive gateway, evaluate `order-approval.dmn`, optionally wait at a manager or director approval user task, then generate invoice/notification work and complete the order. Rejected approvals follow the modeled compensation and rejection path.

## Technology Stack

- Java 17
- Maven 3.9.10 via the included wrapper
- Spring Boot 3.3.7 for business microservices
- Spring Boot 2.7.18 for `workflow-service`
- Camunda Platform 7.19.0 for the embedded workflow engine and web applications
- Spring Boot Actuator
- Micrometer Prometheus registry
- Flyway migrations for services that own local data
- PostgreSQL 11 via Docker Compose for Navicat Premium 16 compatibility
- JUnit 5 and Spring Boot Test

## Startup Instructions

Build Docker images for all runtime services:

```bash
docker compose build
```

Start the full Docker stack:

```bash
docker compose up -d
```

This starts PostgreSQL, Jaeger, OpenTelemetry Collector, and all runtime services:
`workflow-service`, `order-service`, `inventory-service`, `payment-service`,
`shipping-service`, `invoice-service`, `notification-service`, and
`external-task-worker`.

Stop the Docker stack:

```bash
docker compose down
```

If another local stack is already using the default host ports, override only the host bindings:

```powershell
$env:POSTGRES_PORT="15433"
$env:JAEGER_UI_PORT="16687"
$env:OTEL_GRPC_PORT="14317"
$env:OTEL_HTTP_PORT="14318"
docker compose up -d
```

Build and test all modules:

```bash
./mvnw clean verify
```

For Maven-based local development, start only PostgreSQL:

```bash
docker compose up -d postgres
```

PostgreSQL is exposed on `localhost:5433` for local database administration tools.
Default credentials:

```text
Host: localhost
Port: 5433
User: camunda
Password: camunda
Databases: workflow_service, order_service, inventory_service, payment_service, invoice_service
```

Then start an individual service after building:

```bash
./mvnw -pl order-service spring-boot:run
```

Start the workflow service:

```bash
./mvnw -pl workflow-service spring-boot:run
```

## Health Endpoints

Each service exposes:

```text
/actuator/health
/actuator/info
/actuator/metrics
/prometheus
```

Example:

```bash
curl http://localhost:8081/actuator/health
```

Default ports:

| Service | Port |
| --- | ---: |
| `workflow-service` | 8080 |
| `order-service` | 8081 |
| `inventory-service` | 8082 |
| `payment-service` | 8083 |
| `shipping-service` | 8084 |
| `invoice-service` | 8085 |
| `notification-service` | 8086 |
| `external-task-worker` | 8087 |

## OpenTelemetry and Tracing

Local distributed tracing is configured for every Dockerized runtime service with the OpenTelemetry Java Agent, OpenTelemetry Collector, and Jaeger.

When you start the full Docker stack, Jaeger and the collector start with the services automatically:

```bash
docker compose up -d
```

Generate a sample trace:

```bash
curl -X POST http://localhost:8086/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "otel-test-1001",
    "customerId": "customer-otel",
    "channel": "EMAIL",
    "message": "OpenTelemetry smoke test",
    "correlationId": "trace-smoke-1001"
  }'
```

Jaeger API service list:

```bash
curl http://localhost:16686/api/services
```

For Maven-based local development outside Docker, download the Java Agent and run each instrumented service:

```powershell
.\scripts\download-otel-java-agent.ps1
.\scripts\start-otel-service.ps1 -Service workflow-service
.\scripts\start-otel-service.ps1 -Service inventory-service
.\scripts\start-otel-service.ps1 -Service order-service
.\scripts\start-otel-service.ps1 -Service payment-service
```

Jaeger UI:

```text
http://localhost:16686
```

See [docs/observability.md](docs/observability.md) for sample trace requests and the custom business spans added for the Camunda workflow.

## Example API Calls

Start a basic order workflow:

```bash
curl -X POST http://localhost:8080/api/workflows/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-1001",
    "correlationId": "correlation-1001"
  }'
```

In normal use, create the order through `order-service` first so workflow delegates can query the order data by `orderId`.

Create an order through the Order Service. This persists the order and starts the workflow:

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-2001",
    "customerId": "customer-42",
    "orderAmount": 120.50,
    "currency": "USD",
    "sku": "SKU-DEFAULT",
    "quantity": 1,
    "correlationId": "correlation-2001"
  }'
```

Reserve inventory directly with idempotency:

```bash
curl -X POST http://localhost:8082/api/inventory/reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: inventory-reservation-order-2001" \
  -d '{
    "orderId": "order-2001",
    "sku": "SKU-DEFAULT",
    "quantity": 1,
    "correlationId": "correlation-2001"
  }'
```

Charge payment directly with idempotency:

```bash
curl -X POST http://localhost:8083/api/payments/charges \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: payment-charge-order-2001" \
  -d '{
    "orderId": "order-2001",
    "amount": 120.50,
    "currency": "USD",
    "correlationId": "correlation-2001"
  }'
```

Generate an invoice directly with idempotency:

```bash
curl -X POST http://localhost:8085/api/invoices \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: invoice-generation-order-2001" \
  -d '{
    "orderId": "order-2001",
    "amount": 120.50,
    "currency": "USD",
    "correlationId": "correlation-2001"
  }'
```

Publish a notification directly:

```bash
curl -X POST http://localhost:8086/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-2001",
    "customerId": "customer-42",
    "channel": "EMAIL",
    "message": "Order order-2001 has been paid.",
    "correlationId": "correlation-2001"
  }'
```

Simulate a payment decline:

```bash
curl -X POST http://localhost:8083/api/payments/charges \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: payment-charge-order-declined" \
  -d '{
    "orderId": "order-declined",
    "amount": 120.50,
    "currency": "USD",
    "correlationId": "correlation-declined",
    "simulation": "DECLINE"
  }'
```

Simulate a technical payment failure:

```bash
curl -X POST http://localhost:8083/api/payments/charges \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: payment-charge-order-failure" \
  -d '{
    "orderId": "order-failure",
    "amount": 120.50,
    "currency": "USD",
    "correlationId": "correlation-failure",
    "simulation": "TECHNICAL_FAILURE"
  }'
```

Query a workflow by business key:

```bash
curl http://localhost:8080/api/workflows/orders/order-1001
```

List active approval tasks:

```bash
curl http://localhost:8080/api/workflows/orders/tasks
```

Filter approval tasks by candidate group:

```bash
curl "http://localhost:8080/api/workflows/orders/tasks?candidateGroup=managers"
```

Claim an approval task:

```bash
curl -X POST http://localhost:8080/api/workflows/orders/tasks/{taskId}/claim \
  -H "Content-Type: application/json" \
  -d '{
    "assignee": "manager-1"
  }'
```

Complete an approval task:

```bash
curl -X POST http://localhost:8080/api/workflows/orders/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "approver": "manager-1"
  }'
```

## Camunda Cockpit and Tasklist

When `workflow-service` is running:

- Cockpit: <http://localhost:8080/camunda/app/cockpit/default/>
- Tasklist: <http://localhost:8080/camunda/app/tasklist/default/>
- Admin credentials: `demo` / `demo`

## RabbitMQ UI

Not available in Phase 0. RabbitMQ will be introduced in a later phase.

## Testing

Run all current unit tests:

```bash
./mvnw clean verify
```

Current tests prove that each Spring Boot service context starts.

The workflow-service tests also verify:

- BPMN deployment.
- Process completion.
- REST workflow start.
- Insufficient-stock BPMN rejection.
- Payment-decline BPMN rejection.
- Technical payment failure retry behavior.
- Incident generation after payment retries are exhausted.
- Paid-order routing through the exclusive gateway.
- DMN approval level routing for no approval, manager approval, and director approval.
- Manager and director user task creation with candidate groups.
- Approved orders continuing to invoice and notification work.
- Rejected approval tasks following the order rejection path.
- Approval user tasks have interrupting timer timeouts that trigger compensation when no manual decision is completed.
- Approval task list, claim, and complete REST APIs.
- Parallel invoice generation and notification publishing before process completion.
- Notification publishing failure is captured without rolling back invoice generation or blocking order completion.
- External task process definitions deploy.
- External inventory tasks can be fetched and locked by a worker.
- External task failures preserve workflow state and store error message/details for Cockpit.
- Worker handlers complete successful tasks, raise BPMN errors for business failures, and report retryable failures with details.

The order-service, inventory-service, payment-service, invoice-service, and notification-service tests also verify:

- Order creation starts a workflow through the workflow client boundary.
- Duplicate inventory reservation requests with the same `Idempotency-Key` do not decrement stock twice.
- Insufficient stock returns a business conflict.
- Payment success creates a charge transaction.
- Payment decline creates a declined transaction.
- Duplicate payment charge requests with the same `Idempotency-Key` do not create duplicate transactions.
- Technical payment failure simulation returns service unavailable without creating a transaction.
- Invoice generation creates an invoice.
- Duplicate invoice generation requests with the same `Idempotency-Key` do not create duplicate invoices.
- Notification publish success and configured publish failure responses.

## Failure Simulation

Phase 3 supports insufficient-stock simulation by requesting more than the seeded stock for `SKU-DEFAULT`, which starts at quantity `100`.

Payment failure simulation is available through the `payment-service` charge request:

- `simulation: "DECLINE"` returns a declined payment transaction. In the workflow, this is mapped to BPMN error code `PAYMENT_DECLINED` and follows the reject-order path.
- `simulation: "TECHNICAL_FAILURE"` returns HTTP `503` from `payment-service`. In the workflow, technical payment exceptions are retried by Camunda on the async `Charge Payment` job.

Payment behavior can also be configured in `payment-service/src/main/resources/application.yml`:

```yaml
payment:
  processing-delay: 0ms
  decline-above: 10000.00
  technical-failure-order-ids: []
```

Notification failure simulation is available through `notification-service/src/main/resources/application.yml`:

```yaml
notification:
  fail-order-ids: []
```

The BPMN retry cycle for `Charge Payment` is:

```text
R3/PT1S
```

After the configured retries are exhausted, Camunda creates an incident for the failed payment job.

Approval routing is configured in `workflow-service/src/main/resources/processes/order-approval.dmn`:

| Order amount | Approval level |
| ---: | --- |
| `< 5000` | `NONE` |
| `5000` through `49999.99` | `MANAGER` |
| `>= 50000` | `DIRECTOR` |


Approval user task timeout:

```text
PT10M
```

When an order requires `MANAGER` or `DIRECTOR` approval, the process waits at the user task for up to ten minutes. During that window, complete the task through the approval API with `approved: true` or `approved: false`. If the timer fires first, the user task is interrupted, `approved` is set to `false`, `failureReason` is set to an approval-timeout message, and the workflow continues through compensation and rejection.
## Learning Roadmap

See [docs/learning-roadmap.md](docs/learning-roadmap.md).

## External Tasks

The default API still starts `order-processing`. Phase 9 adds a second implementation branch for learning and comparison:

```text
order-processing-external
  reserve-inventory -> topic reserve-inventory
  payment-subprocess-external -> topic charge-payment
  shipping-subprocess-external -> topic create-shipment
```

Run the worker locally after `workflow-service` is available:

```bash
./mvnw -pl external-task-worker spring-boot:run
```

Stop or restart `external-task-worker` independently to observe that locked tasks remain in Camunda and are retried after the lock expires. Technical failures are reported with retry counts and stack details so Cockpit can show the failure information.

## Known Limitations

- The payment task is asynchronous so Camunda can retry technical failures and create incidents.
- External-task workflow start is currently available through Camunda REST/API by process key; the public order workflow REST endpoint preserves the default `order-processing` behavior.
- Notification publishing is intentionally non-critical; failures are recorded in process variables and the workflow continues.
- Runtime services use PostgreSQL by default; Maven tests use the `test` profile with in-memory H2 databases.
- Docker Compose starts PostgreSQL for the workflow engine and persisted business services.
- Authentication and authorization are only development-level Camunda webapp credentials.
