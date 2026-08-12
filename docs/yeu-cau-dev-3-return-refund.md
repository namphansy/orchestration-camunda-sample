# Yêu Cầu Dev 3 - Quy Trình Trả Hàng Và Hoàn Tiền

## Mục Tiêu

Xây dựng một nghiệp vụ độc lập để học cách dùng Camunda 7 với Spring Boot cho các chủ đề:

- Saga orchestration.
- Compensation và nhánh hoàn tác nghiệp vụ.
- Service task retry và incident khi lỗi kỹ thuật.
- Message correlation cho sự kiện nhận hàng và kiểm định.
- Tích hợp nhiều service trong một quy trình.

Nghiệp vụ này phải được triển khai như một process mới, không sửa trực tiếp luồng `order-processing` hiện có.

## Bối Cảnh Nghiệp Vụ

Khách hàng có thể yêu cầu trả hàng cho một đơn hàng đã hoàn tất. Hệ thống cần:

- Kiểm tra đơn hàng có tồn tại và có đủ điều kiện trả hàng.
- Tạo vận đơn trả hàng.
- Chờ sự kiện kho nhận lại hàng.
- Chờ kết quả kiểm định hàng trả.
- Nếu được chấp nhận, hoàn tiền và cập nhật tồn kho nếu cần.
- Nếu bị từ chối, không hoàn tiền và thông báo cho khách hàng.

Nếu khách không gửi hàng về trong thời gian SLA, yêu cầu trả hàng hết hạn.

## Phạm Vi Triển Khai

Tạo process mới:

```text
workflow-service/src/main/resources/processes/return-refund.bpmn
```

Tạo API trong `workflow-service`:

```text
POST /api/workflows/returns
GET /api/workflows/returns/{businessKey}
POST /api/workflows/returns/{businessKey}/received-events
POST /api/workflows/returns/{businessKey}/inspection-events
```

Tạo các class chính:

```text
workflow-service/src/main/java/com/example/workflow/api/controller/ReturnWorkflowController.java
workflow-service/src/main/java/com/example/workflow/application/service/ReturnWorkflowService.java
workflow-service/src/test/java/com/example/workflow/application/service/ReturnWorkflowProcessTests.java
```

Có thể mở rộng các service sau nếu cần:

```text
shipping-service/src/main/java/com/example/shipping/api/controller/ReturnShipmentController.java
inventory-service/src/main/java/com/example/inventory/api/controller/InventoryRestockController.java
payment-service/src/main/java/com/example/payment/api/controller/PaymentController.java
```

Ưu tiên tái sử dụng logic refund hiện có trong `payment-service` nếu đã có.

## Luồng BPMN Đề Xuất

```text
Start Return
  -> Validate Return Eligibility
  -> Create Return Shipment
  -> Wait For ReturnItemReceived message
     -> Nếu timeout:
        -> Mark Return Expired
        -> Notify Customer
        -> Return Expired
     -> Nếu nhận hàng:
        -> Wait For ReturnInspectionCompleted message
        -> Gateway theo inspectionAccepted
           -> accepted=true:
              -> Refund Payment
              -> Restock Inventory nếu cần
              -> Notify Customer
              -> Return Refunded
           -> accepted=false:
              -> Notify Customer
              -> Return Rejected
```

Message catch events:

```text
ReturnItemReceived
ReturnInspectionCompleted
```

Timer timeout gợi ý:

```text
PT7D
```

Retry gợi ý cho task hoàn tiền:

```text
R3/PT1S
```

## Process Variables

Sử dụng các biến sau:

```text
returnId
orderId
customerId
returnReason
returnStatus
returnShipmentId
refundTransactionId
inspectionAccepted
failureReason
correlationId
```

Giá trị hợp lệ cho `returnStatus`:

```text
REQUESTED
WAITING_FOR_ITEM
INSPECTION
ACCEPTED
REJECTED
REFUNDED
EXPIRED
FAILED
```

## API Request Gợi Ý

Start return:

```json
{
  "returnId": "return-1001",
  "orderId": "order-1001",
  "customerId": "customer-42",
  "returnReason": "Sản phẩm bị lỗi",
  "correlationId": "correlation-1001"
}
```

Return item received event:

```json
{
  "receivedBy": "warehouse-1",
  "receivedAt": "2026-08-12T10:00:00Z",
  "correlationId": "correlation-1001"
}
```

Inspection event:

```json
{
  "inspectionAccepted": true,
  "inspector": "inspector-1",
  "comment": "Hàng còn đủ điều kiện hoàn tiền",
  "correlationId": "correlation-1001"
}
```

## Tiêu Chí Nghiệm Thu

- Process `return-refund` deploy thành công cùng `workflow-service`.
- API start return kiểm tra được order tồn tại trước khi tạo process.
- Process tạo return shipment hoặc gọi mock/service tương ứng.
- Process chờ message `ReturnItemReceived`.
- Nếu quá hạn trước khi nhận hàng, set `returnStatus=EXPIRED` và gửi notification.
- Event nhận hàng đúng `correlationId` đưa process sang bước kiểm định.
- Event kiểm định với `inspectionAccepted=true` kích hoạt hoàn tiền.
- Hoàn tiền thành công thì set `returnStatus=REFUNDED` và lưu `refundTransactionId`.
- Event kiểm định với `inspectionAccepted=false` bỏ qua hoàn tiền, set `returnStatus=REJECTED`, và gửi notification.
- Lỗi kỹ thuật khi hoàn tiền phải dùng Camunda retry.
- Khi retry hoàn tiền hết số lần, Camunda tạo incident.
- Có test cho các case: happy path, rejected inspection, timeout trước khi nhận hàng, refund technical retry, incident sau khi hết retry.

## Validation Bắt Buộc

Chạy tối thiểu:

```bash
./mvnw -pl workflow-service test
```

Nếu có sửa service khác, chạy thêm module tương ứng:

```bash
./mvnw -pl payment-service test
./mvnw -pl shipping-service test
./mvnw -pl inventory-service test
```

Trước khi bàn giao branch, chạy:

```bash
./mvnw clean verify
```

## Branch Đề Xuất

```text
feature/return-refund-workflow
```

## Lưu Ý Khi Làm

- Không sửa logic chính của `order-processing.bpmn`.
- Thiết kế process theo hướng dễ quan sát trong Camunda Cockpit.
- Các message event phải kiểm tra `correlationId`.
- Không hoàn tiền hai lần nếu nhận duplicate inspection event.
- Tách rõ lỗi nghiệp vụ và lỗi kỹ thuật: lỗi nghiệp vụ đi theo nhánh BPMN, lỗi kỹ thuật để Camunda retry.
