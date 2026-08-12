# Yêu Cầu Dev 2 - Quy Trình Xử Lý Đơn Hàng Chờ Bổ Sung Tồn Kho

## Mục Tiêu

Xây dựng một nghiệp vụ độc lập để học cách dùng Camunda 7 với Spring Boot cho các chủ đề:

- Message correlation.
- Process chờ sự kiện nghiệp vụ.
- Timer timeout cho luồng chờ.
- Idempotency khi nhận sự kiện trùng.
- Tích hợp `workflow-service` với `inventory-service`.

Nghiệp vụ này phải được triển khai như một process mới, không sửa trực tiếp luồng `order-processing` hiện có.

## Bối Cảnh Nghiệp Vụ

Khi tồn kho không đủ, một số đơn hàng không nên bị từ chối ngay. Hệ thống có thể tạo một backorder và chờ sự kiện bổ sung tồn kho.

Nếu có restock trước thời hạn SLA, hệ thống tiếp tục reserve inventory. Nếu không có restock trước thời hạn, backorder hết hạn và kết thúc.

## Phạm Vi Triển Khai

Tạo process mới:

```text
workflow-service/src/main/resources/processes/backorder-fulfillment.bpmn
```

Tạo API trong `workflow-service`:

```text
POST /api/workflows/backorders
GET /api/workflows/backorders/{businessKey}
POST /api/workflows/backorders/{businessKey}/restock-events
```

Tạo các class chính:

```text
workflow-service/src/main/java/com/example/workflow/api/controller/BackorderWorkflowController.java
workflow-service/src/main/java/com/example/workflow/application/service/BackorderWorkflowService.java
workflow-service/src/test/java/com/example/workflow/application/service/BackorderWorkflowProcessTests.java
```

Nếu cần mở rộng inventory, tạo thêm:

```text
inventory-service/src/main/java/com/example/inventory/api/controller/InventoryRestockController.java
```

## Luồng BPMN Đề Xuất

```text
Start Backorder
  -> Mark Backorder Waiting For Restock
  -> Wait For RestockReceived message
     -> Nếu nhận restock đúng correlationId:
        -> Reserve Inventory
        -> Mark Backorder Reserved
        -> Backorder Completed
     -> Nếu timeout:
        -> Mark Backorder Expired
        -> Backorder Expired
```

Message catch event:

```text
RestockReceived
```

Timer timeout gợi ý:

```text
PT30M
```

Có thể dùng boundary timer trên message catch event hoặc event-based gateway tùy cách model BPMN.

## Chiến Lược Correlation

Restock event phải được correlate bằng:

```text
businessKey = backorderId
correlationId = process variable correlationId
```

Nếu event gửi vào sai `correlationId`, API phải trả kết quả bị bỏ qua, không làm process chạy tiếp.

Nếu event gửi trùng sau khi process đã xử lý, API phải xử lý idempotent và không reserve inventory lần thứ hai.

## Process Variables

Sử dụng các biến sau:

```text
backorderId
orderId
sku
quantity
backorderStatus
inventoryReservationId
restockReceived
failureReason
correlationId
```

Giá trị hợp lệ cho `backorderStatus`:

```text
WAITING_FOR_RESTOCK
RESERVED
EXPIRED
FAILED
```

## API Request Gợi Ý

Start backorder:

```json
{
  "backorderId": "backorder-1001",
  "orderId": "order-1001",
  "sku": "SKU-DEFAULT",
  "quantity": 2,
  "correlationId": "correlation-1001"
}
```

Restock event:

```json
{
  "sku": "SKU-DEFAULT",
  "quantity": 10,
  "correlationId": "correlation-1001"
}
```

Inventory restock API nếu cần:

```text
POST /api/inventory/restocks
```

Body:

```json
{
  "sku": "SKU-DEFAULT",
  "quantity": 10,
  "correlationId": "correlation-1001"
}
```

## Tiêu Chí Nghiệm Thu

- Process `backorder-fulfillment` deploy thành công cùng `workflow-service`.
- API start backorder tạo process instance và đưa process vào trạng thái chờ message `RestockReceived`.
- API query trả được trạng thái hiện tại của backorder.
- Restock event đúng `businessKey` và đúng `correlationId` làm process chạy tiếp.
- Sau khi nhận restock hợp lệ, process gọi inventory reservation API với `Idempotency-Key`.
- Reserve inventory thành công thì set `backorderStatus=RESERVED`.
- Restock event sai `correlationId` bị bỏ qua và không làm process chạy tiếp.
- Restock event trùng không reserve inventory lần thứ hai.
- Nếu quá hạn timer, process set `backorderStatus=EXPIRED` và không reserve inventory.
- Có test cho các case: start và wait, correlation thành công, correlation sai, duplicate event, timeout.

## Validation Bắt Buộc

Chạy tối thiểu:

```bash
./mvnw -pl workflow-service test
```

Nếu có sửa `inventory-service`, chạy thêm:

```bash
./mvnw -pl inventory-service test
```

Trước khi bàn giao branch, chạy:

```bash
./mvnw clean verify
```

## Branch Đề Xuất

```text
feature/backorder-fulfillment-workflow
```

## Lưu Ý Khi Làm

- Không sửa logic chính của `order-processing.bpmn`.
- Cẩn thận idempotency để không trừ tồn kho hai lần.
- Tách endpoint backorder khỏi endpoint order workflow hiện có.
- Message correlation nên trả kết quả rõ ràng: `CORRELATED`, `IGNORED`, hoặc `ALREADY_COMPLETED`.
