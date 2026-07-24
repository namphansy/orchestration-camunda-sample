# Codex Project Prompt — Camunda 7 Microservice Orchestration Learning Project

## 1. Role

You are a senior Java backend engineer and solution architect.

Your task is to build a production-oriented learning project demonstrating how Camunda 7 orchestrates a distributed order-processing workflow across multiple Spring Boot microservices.

Do not generate the entire project in one uncontrolled step. Work incrementally, validate each phase, and keep the project runnable after every milestone.

---

## 2. Project Goal

Build an **Order Processing System** using:

- Java 17
- Spring Boot 3.x for business microservices
- Spring Boot 2.7.x for the embedded Camunda 7 workflow service, if required by the selected Camunda version
- Camunda Platform 7
- BPMN 2.0
- DMN
- PostgreSQL
- RabbitMQ
- Docker Compose
- Maven
- REST APIs
- Testcontainers
- JUnit 5
- Mockito
- OpenAPI/Swagger
- Prometheus-compatible Actuator metrics

The system must demonstrate:

- BPMN process execution
- Process variables
- Service Tasks
- External Tasks
- User Tasks
- Exclusive and Parallel Gateways
- Timer Events
- Message Events
- Error Events
- Retry and Incident handling
- Saga compensation
- Call Activities
- Multi-instance activities
- DMN-based approval decisions
- REST and asynchronous messaging integration
- Idempotency
- Correlation IDs
- Observability
- Docker-based local deployment

---

## 3. Business Scenario

The project processes customer orders.

A normal order flow is:

1. Create an order.
2. Validate the order.
3. Reserve inventory.
4. Charge payment.
5. Decide whether manual approval is required.
6. Generate an invoice.
7. Arrange shipment.
8. Send notifications.
9. Complete the order.

Failure behavior:

- If inventory reservation fails, reject the order.
- If payment fails, release inventory and cancel the order.
- If shipment creation fails after payment succeeds, refund payment, release inventory, and cancel the order.
- If payment confirmation does not arrive within a configured timeout, cancel the process.
- Technical failures must be retried.
- Business errors must be handled explicitly in BPMN.
- Every external command must be idempotent.

Approval behavior:

- Small orders do not require approval.
- Medium-value orders require manager approval.
- High-value orders require director approval.
- Approval rules must be defined in DMN, not hardcoded in Java.

---

## 4. Target Repository Structure

Create a Maven multi-module repository with this structure:

```text
camunda-order-orchestration/
├── README.md
├── pom.xml
├── docker-compose.yml
├── docs/
│   ├── architecture.md
│   ├── api-contracts.md
│   ├── workflow-design.md
│   ├── failure-scenarios.md
│   └── learning-roadmap.md
├── infrastructure/
│   ├── postgres/
│   ├── rabbitmq/
│   └── monitoring/
├── workflow-service/
├── order-service/
├── inventory-service/
├── payment-service/
├── shipping-service/
├── invoice-service/
├── notification-service/
└── integration-tests/
```

Each service must be independently buildable and runnable.

---

## 5. Service Responsibilities

### 5.1 Workflow Service

Responsibilities:

- Host the Camunda 7 process engine.
- Deploy BPMN and DMN resources.
- Start order processes.
- Store workflow state.
- Invoke or coordinate business services.
- Correlate asynchronous messages.
- Expose APIs for process operations.
- Implement External Task workers only when the learning phase specifically requires them.
- Never own business data that belongs to another service.

Suggested endpoints:

```text
POST /api/workflows/orders
GET  /api/workflows/orders/{businessKey}
POST /api/workflows/orders/{businessKey}/messages/payment-confirmed
POST /api/workflows/orders/{businessKey}/messages/shipment-confirmed
```

Required process variables:

```text
orderId
customerId
businessKey
correlationId
orderAmount
currency
approvalLevel
inventoryReservationId
paymentTransactionId
shipmentId
invoiceId
orderStatus
paymentStatus
inventoryStatus
shipmentStatus
failureReason
```

### 5.2 Order Service

Responsibilities:

- Own order data.
- Create and update orders.
- Maintain the business order status.
- Start the Camunda workflow through the Workflow Service.
- Expose order query APIs.
- Avoid implementing orchestration logic.

Suggested statuses:

```text
CREATED
VALIDATED
PROCESSING
WAITING_FOR_APPROVAL
APPROVED
REJECTED
COMPLETED
CANCELLED
FAILED
```

### 5.3 Inventory Service

Responsibilities:

- Reserve stock.
- Release stock.
- Prevent duplicate reservations.
- Return a business error when stock is insufficient.

Suggested endpoints:

```text
POST /api/inventory/reservations
DELETE /api/inventory/reservations/{reservationId}
GET /api/inventory/reservations/{reservationId}
```

### 5.4 Payment Service

Responsibilities:

- Authorize or charge payment.
- Refund payment.
- Support synchronous and asynchronous payment modes.
- Prevent duplicate charges and refunds.
- Simulate timeout and technical failures.

Suggested endpoints:

```text
POST /api/payments
POST /api/payments/{transactionId}/refund
GET  /api/payments/{transactionId}
```

### 5.5 Shipping Service

Responsibilities:

- Create shipments.
- Cancel shipments.
- Simulate carrier failures.
- Ensure idempotent shipment creation.

Suggested endpoints:

```text
POST /api/shipments
POST /api/shipments/{shipmentId}/cancel
GET  /api/shipments/{shipmentId}
```

### 5.6 Invoice Service

Responsibilities:

- Generate invoices.
- Return an invoice ID.
- Avoid generating duplicate invoices for the same order.

Suggested endpoints:

```text
POST /api/invoices
GET  /api/invoices/{invoiceId}
```

### 5.7 Notification Service

Responsibilities:

- Send email and SMS notifications.
- Consume notification events from RabbitMQ.
- Store delivery attempts.
- Retry failed deliveries.
- Never block the main workflow for non-critical notifications.

Suggested event types:

```text
ORDER_CREATED
ORDER_APPROVAL_REQUIRED
ORDER_APPROVED
ORDER_REJECTED
ORDER_COMPLETED
ORDER_CANCELLED
PAYMENT_FAILED
SHIPMENT_FAILED
```

---

## 6. Architecture Rules

Apply the following rules consistently:

1. Each service owns its own database schema.
2. Services must not access another service's tables.
3. Workflow Service owns process state, not business entity state.
4. Order Service owns the order aggregate.
5. Use REST for simple synchronous commands in early phases.
6. Introduce RabbitMQ for asynchronous events in later phases.
7. Use correlation IDs in every request, log, event, and process instance.
8. Use the order ID as the Camunda business key.
9. Every command endpoint must support idempotency.
10. Business errors and technical errors must be modeled differently.
11. Do not use distributed database transactions.
12. Use Saga compensation for cross-service rollback.
13. Do not hide orchestration inside controllers.
14. Keep BPMN delegates thin.
15. Put reusable integration logic in dedicated clients or adapters.
16. Add timeouts to all remote calls.
17. Do not use infinite retries.
18. Avoid storing large payloads as Camunda process variables.
19. Never store secrets in source code.
20. Keep the code readable for a developer learning Camunda.

---

## 7. Required Technical Patterns

### 7.1 Layering

Use this package structure where applicable:

```text
com.example.<service>
├── api
│   ├── controller
│   ├── request
│   └── response
├── application
│   ├── service
│   ├── command
│   └── query
├── domain
│   ├── model
│   ├── repository
│   └── exception
├── infrastructure
│   ├── persistence
│   ├── messaging
│   ├── client
│   └── configuration
└── shared
    ├── error
    ├── logging
    └── idempotency
```

Do not over-engineer simple modules, but maintain clear separation between API, application, domain, and infrastructure code.

### 7.2 API Error Response

Use one common error response format:

```json
{
  "timestamp": "2026-07-17T10:00:00Z",
  "service": "payment-service",
  "correlationId": "uuid",
  "errorCode": "PAYMENT_DECLINED",
  "message": "Payment was declined",
  "details": {}
}
```

### 7.3 Event Envelope

Use this RabbitMQ event envelope:

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_COMPLETED",
  "eventVersion": 1,
  "occurredAt": "2026-07-17T10:00:00Z",
  "producer": "workflow-service",
  "correlationId": "uuid",
  "businessKey": "order-id",
  "payload": {}
}
```

### 7.4 Idempotency

For command APIs:

- Accept `Idempotency-Key`.
- Store processed command keys.
- Return the previous successful response for duplicate requests.
- Add tests for duplicate inventory reservation, payment charge, refund, shipment, and invoice generation.

### 7.5 Observability

Every service must provide:

```text
/actuator/health
/actuator/info
/actuator/metrics
/prometheus
```

Log fields:

```text
timestamp
level
service
correlationId
businessKey
processInstanceId
activityId
message
```

---

## 8. BPMN Design

Create a main process:

```text
order-processing.bpmn
```

The target process should eventually contain:

```text
Start Order
  -> Validate Order
  -> Reserve Inventory
  -> Charge Payment
  -> Determine Approval Requirement using DMN
  -> Approval Gateway
      -> No Approval
      -> Manager User Task
      -> Director User Task
  -> Parallel Gateway
      -> Generate Invoice
      -> Publish Notification
  -> Create Shipment
  -> Wait for Shipment Confirmation
  -> Complete Order
```

Failure paths:

```text
Inventory unavailable
  -> Reject Order

Payment declined
  -> Release Inventory
  -> Cancel Order

Payment technical failure
  -> Retry
  -> Incident after retries are exhausted

Payment timeout
  -> Cancel Order
  -> Release Inventory

Shipment failure
  -> Refund Payment
  -> Release Inventory
  -> Cancel Order
```

Use BPMN Error Events for business failures.

Use failed jobs and retry cycles for technical failures.

Use compensation or explicit compensation tasks for Saga rollback.

---

## 9. DMN Design

Create:

```text
order-approval.dmn
```

Example decision table:

| Minimum Amount | Maximum Amount | Approval Level |
|---:|---:|---|
| 0 | 4,999.99 | NONE |
| 5,000 | 49,999.99 | MANAGER |
| 50,000 | unlimited | DIRECTOR |

Use FEEL-compatible expressions supported by the selected Camunda 7 setup.

Do not implement approval thresholds in Java.

---

## 10. Implementation Roadmap

Codex must execute the project in phases.

After every phase:

1. Build the affected Maven modules.
2. Run unit tests.
3. Run integration tests when available.
4. Update the README.
5. Summarize generated files.
6. Report remaining limitations.
7. Do not start the next phase until the current phase is compilable.

### Phase 0 — Repository Bootstrap

Create:

- Parent Maven project.
- Module structure.
- Shared dependency management.
- README.
- Docker Compose with PostgreSQL.
- Basic health endpoints.
- Formatting and test configuration.
- `.gitignore`.
- `.editorconfig`.

Acceptance criteria:

- `mvn clean verify` succeeds.
- All services start.
- Health endpoints return `UP`.

### Phase 1 — Basic Camunda Workflow

Implement:

- Workflow Service with embedded Camunda 7.
- A basic BPMN flow.
- API to start a process.
- Process variables.
- Business key.
- Simple Java Delegates that only log actions.

Acceptance criteria:

- A process can be started through REST.
- It completes successfully.
- The process is visible in Cockpit.
- Tests verify process deployment and completion.

### Phase 2 — Order and Inventory Services

Implement:

- Order persistence.
- Order creation API.
- Inventory reservation API.
- REST integration from Workflow Service.
- Insufficient-stock BPMN error path.
- Idempotent inventory reservation.

Acceptance criteria:

- Creating an order starts a workflow.
- Stock can be reserved.
- Insufficient stock rejects the order.
- Duplicate reservation requests do not decrement stock twice.

### Phase 3 — Payment Integration

Implement:

- Payment Service.
- Payment success and decline scenarios.
- Configurable artificial delay.
- Technical failure simulation.
- Camunda retry configuration.
- Incident generation after retries are exhausted.
- Payment business-error handling.

Acceptance criteria:

- Payment decline follows a modeled BPMN path.
- Technical failure is retried.
- Exhausted retries create an incident.
- Duplicate charge requests do not create duplicate transactions.

### Phase 4 — Gateways and Parallel Work

Implement:

- Exclusive Gateway for payment result.
- Parallel Gateway for invoice generation and notification publishing.
- Invoice Service.
- Notification Service skeleton.

Acceptance criteria:

- Parallel branches both complete before process continuation.
- Invoice generation is idempotent.
- Notification failures do not corrupt invoice processing.

### Phase 5 — User Tasks and DMN

Implement:

- DMN approval decision.
- Manager approval User Task.
- Director approval User Task.
- APIs to list, claim, and complete tasks.
- Candidate groups.
- Approval and rejection paths.

Acceptance criteria:

- Approval level is decided by DMN.
- Correct User Task is created.
- Approved orders continue.
- Rejected orders trigger cancellation and compensation.

### Phase 6 — Timers and Asynchronous Messages

Implement:

- Payment confirmation message event.
- Boundary Timer Event.
- Message correlation using business key and correlation ID.
- Asynchronous payment mode.

Acceptance criteria:

- Payment confirmation resumes the correct process.
- Unknown messages are rejected or safely ignored.
- Timeout triggers cancellation.
- Duplicate messages do not advance the process twice.

### Phase 7 — Shipping and Saga Compensation

Implement:

- Shipping Service.
- Shipment creation.
- Shipment failure.
- Payment refund.
- Inventory release.
- Compensation workflow.

Acceptance criteria:

- Shipment failure causes refund and inventory release.
- Compensation operations are idempotent.
- Repeated compensation does not duplicate refunds.
- Final order status is consistent.

### Phase 8 — Call Activities and Multi-instance

Refactor the process into:

```text
order-processing.bpmn
payment-subprocess.bpmn
shipping-subprocess.bpmn
notification-subprocess.bpmn
```

Implement:

- Call Activities.
- Input/output variable mappings.
- Multi-instance inventory reservation for order lines.

Acceptance criteria:

- Main workflow is readable.
- Child processes are reusable.
- Multi-item orders reserve each product.
- One failed reservation triggers the expected rollback behavior.

### Phase 9 — External Tasks

Create a second workflow implementation or branch using External Tasks for:

- Inventory reservation.
- Payment.
- Shipment.

Create independently deployable workers.

Acceptance criteria:

- Workers can be stopped and restarted.
- Locked tasks are eventually retried.
- Worker failure does not lose workflow state.
- External Task failure details are visible in Cockpit.

### Phase 10 — RabbitMQ Integration

Implement:

- Domain event publishing.
- Notification event consumption.
- Dead-letter exchange.
- Retry queues.
- Event idempotency.
- Correlation metadata.

Acceptance criteria:

- Notifications are processed asynchronously.
- Poison messages reach the dead-letter queue.
- Duplicate events are ignored.
- Workflow completion does not depend on successful non-critical notifications.

### Phase 11 — Testing and Observability

Implement:

- Unit tests.
- Controller tests.
- Repository tests.
- Camunda process tests.
- Testcontainers integration tests.
- End-to-end happy-path test.
- End-to-end compensation test.
- Structured logging.
- Metrics.
- Health checks.

Minimum test scenarios:

1. Happy path without approval.
2. Manager approval.
3. Director approval.
4. Approval rejection.
5. Insufficient stock.
6. Payment decline.
7. Payment timeout.
8. Payment technical failure and incident.
9. Shipment failure and compensation.
10. Duplicate payment command.
11. Duplicate payment confirmation message.
12. Duplicate RabbitMQ event.
13. Multi-item inventory reservation failure.
14. External worker restart.

### Phase 12 — Docker and Documentation

Implement:

- Complete Docker Compose.
- PostgreSQL databases or schemas.
- RabbitMQ management UI.
- Camunda Cockpit and Tasklist.
- Service health checks.
- Startup dependency handling.
- Local run instructions.
- Architecture diagrams using Mermaid.
- Troubleshooting guide.

Acceptance criteria:

- `docker compose up --build` starts the entire system.
- A sample order can be processed using documented curl commands.
- Cockpit shows the process.
- Tasklist shows approval tasks.
- RabbitMQ UI shows exchanges and queues.
- README explains all learning concepts.

---

## 11. Testing Standards

Use:

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- MockMvc or WebTestClient
- Testcontainers
- Camunda BPM Assert where compatible

Rules:

- Every business rule requires a test.
- Every compensation action requires an idempotency test.
- Do not mock the process engine in process-level tests.
- Use Testcontainers for PostgreSQL and RabbitMQ integration tests.
- Test both success and failure paths.
- Use deterministic test data.
- Avoid `Thread.sleep` where polling or Awaitility is more appropriate.

---

## 12. Code Quality Rules

- Prefer constructor injection.
- Avoid field injection.
- Use records for immutable request/response DTOs when supported.
- Use enums for statuses and error codes.
- Use database migrations with Flyway.
- Do not use `ddl-auto=create` outside tests.
- Do not expose JPA entities directly from controllers.
- Add validation using Jakarta Bean Validation.
- Add global exception handling.
- Add meaningful logs without sensitive data.
- Configure REST client connection and read timeouts.
- Add resilience only where required; avoid hiding failures with excessive fallback logic.
- Keep methods focused.
- Avoid unnecessary inheritance.
- Document non-obvious Camunda behavior.
- Do not add Lombok unless there is a clear project-wide reason.

---

## 13. Camunda-Specific Rules

- Use the order ID as the process business key.
- Keep Java Delegates stateless.
- Do not place business logic directly inside delegates.
- Use typed process variable access helpers.
- Document all process variables.
- Avoid changing variable names without migration notes.
- Use BPMN Error for expected business exceptions.
- Allow unexpected technical exceptions to fail the job.
- Configure bounded retry cycles, for example:

```text
R3/PT10S
```

- Model timeout behavior explicitly.
- Use message names and correlation keys consistently.
- Keep process versioning in mind.
- Do not delete old BPMN definitions that may still have running instances.
- Explain process migration implications whenever BPMN structure changes.

---

## 14. Security Scope

For this learning project:

- Use simple Basic Authentication or development-only credentials for Camunda web applications.
- Do not implement a full production IAM solution.
- Keep credentials in environment variables.
- Never commit passwords.
- Clearly label development-only security configurations.
- Protect task completion and workflow administration APIs with simple roles where practical.

---

## 15. Required Documentation

Generate and maintain:

### README.md

Must include:

- Project purpose.
- Architecture overview.
- Module descriptions.
- Technology stack.
- Startup instructions.
- Example API calls.
- Camunda Cockpit access.
- Camunda Tasklist access.
- RabbitMQ UI access.
- Testing instructions.
- Failure simulation instructions.
- Learning roadmap.
- Known limitations.

### docs/architecture.md

Must include:

- System context.
- Service boundaries.
- Data ownership.
- Synchronous communication.
- Asynchronous communication.
- Saga design.
- Mermaid diagrams.

### docs/workflow-design.md

Must include:

- BPMN activity descriptions.
- Process variables.
- Message correlation strategy.
- Error handling strategy.
- Retry strategy.
- Compensation strategy.
- Process versioning notes.

### docs/api-contracts.md

Must include:

- Request and response examples.
- Headers.
- Error codes.
- Idempotency behavior.
- Correlation rules.

### docs/failure-scenarios.md

Must explain how to simulate:

- Insufficient stock.
- Payment decline.
- Payment timeout.
- Technical payment failure.
- Shipment failure.
- Notification failure.
- RabbitMQ poison message.
- External Task worker crash.

---

## 16. Codex Working Protocol

For every requested phase, follow this workflow:

1. Inspect the existing repository.
2. Summarize the current state.
3. State the exact files to create or modify.
4. Implement only the requested phase.
5. Build the project.
6. Run relevant tests.
7. Fix compilation and test failures.
8. Show a concise change summary.
9. Show commands used for validation.
10. List any assumptions and remaining work.

Do not:

- Rewrite unrelated modules.
- Change public contracts without explanation.
- Skip tests.
- Leave placeholder methods that silently return success.
- Claim commands succeeded unless they were actually executed.
- Generate fake logs or fake test results.
- Introduce Kubernetes before Docker Compose works.
- Add infrastructure that is not used.

---

## 17. Initial Codex Task

Start with **Phase 0 only**.

Create the repository skeleton and the initial Maven multi-module configuration.

Requirements for Phase 0:

- Root Maven parent project.
- All service modules.
- Java version configuration.
- Dependency management.
- Basic Spring Boot applications.
- Health endpoints.
- Flyway setup placeholders where databases will be used.
- Docker Compose with PostgreSQL.
- `.gitignore`.
- `.editorconfig`.
- Initial README.
- Basic unit test proving each Spring context starts.
- Maven Wrapper.

Before writing code:

1. Present the proposed module dependency graph.
2. Identify Camunda 7 and Spring Boot compatibility constraints.
3. Select concrete framework versions.
4. Explain any module that must use a different Spring Boot version.
5. Keep the build reproducible.

After implementation:

1. Run `./mvnw clean verify`.
2. Report the test results.
3. Show the generated repository tree.
4. Stop after Phase 0.

---

## 18. Follow-up Prompt Template

Use the following prompt for each later phase:

```text
Implement Phase <NUMBER>: <PHASE NAME> from the project specification.

Before coding:
1. Inspect the current repository.
2. Confirm which acceptance criteria are already satisfied.
3. List the files that will be added or modified.
4. Identify compatibility or migration risks.

During implementation:
1. Keep all existing tests passing.
2. Add tests for new behavior.
3. Preserve API and BPMN backward compatibility unless the phase explicitly requires a change.
4. Update documentation.

After implementation:
1. Run the affected module tests.
2. Run ./mvnw clean verify.
3. Summarize code changes.
4. List commands used.
5. Report test results honestly.
6. List remaining limitations.
7. Stop after completing this phase.
```

---

## 19. Bug-Fix Prompt Template

```text
Investigate and fix the following issue:

<DESCRIBE THE ISSUE>

Required workflow:
1. Reproduce the issue.
2. Identify the root cause.
3. Explain whether it is a business-flow, BPMN, transaction, concurrency, integration, or infrastructure issue.
4. Implement the smallest safe fix.
5. Add a regression test.
6. Run relevant tests.
7. Explain any impact on running Camunda process instances.
8. Do not modify unrelated behavior.
```

---

## 20. Review Prompt Template

```text
Review the current implementation of <MODULE OR FEATURE>.

Focus on:
- Camunda modeling correctness
- Transaction boundaries
- Idempotency
- Retry behavior
- Business errors versus technical failures
- Message correlation
- Process-variable design
- Saga compensation
- Service ownership boundaries
- REST client timeouts
- RabbitMQ delivery guarantees
- Test coverage
- Security
- Observability
- Production risks

Return:
1. Critical issues.
2. High-priority issues.
3. Medium-priority improvements.
4. Positive design decisions.
5. A prioritized remediation plan.

Do not change code unless explicitly requested.
```
