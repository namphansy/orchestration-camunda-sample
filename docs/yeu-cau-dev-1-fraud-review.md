# Yêu Cầu Dev 1 - Quy Trình Rà Soát Gian Lận

## Mục Tiêu

Xây dựng một nghiệp vụ độc lập để học cách dùng Camunda 7 với Spring Boot cho các chủ đề:

- DMN decision table để phân loại rủi ro.
- User task cho nghiệp vụ cần người duyệt.
- Candidate group, claim task, complete task.
- Boundary timer để tự động xử lý khi quá hạn.
- Process variables và lịch sử trạng thái trong Camunda.

Nghiệp vụ này phải được triển khai như một process mới, không sửa trực tiếp luồng `order-processing` hiện có.

## Bối Cảnh Nghiệp Vụ

Một số đơn hàng cần được rà soát gian lận trước khi tiếp tục xử lý. Hệ thống nhận thông tin đơn hàng và điểm rủi ro gian lận (`fraudScore`), sau đó dùng DMN để quyết định:

- Rủi ro thấp: tự động phê duyệt.
- Rủi ro trung bình: chờ nhân viên phân tích gian lận duyệt.
- Rủi ro cao: chờ quản lý gian lận duyệt.

Nếu user task không được xử lý trong thời gian SLA, hệ thống tự động từ chối yêu cầu rà soát.

## Phạm Vi Triển Khai

Tạo process mới:

```text
workflow-service/src/main/resources/processes/fraud-review.bpmn
```

Tạo DMN mới:

```text
workflow-service/src/main/resources/processes/fraud-risk.dmn
```

Tạo API trong `workflow-service`:

```text
POST /api/workflows/fraud-reviews
GET /api/workflows/fraud-reviews/{businessKey}
GET /api/workflows/fraud-reviews/tasks
POST /api/workflows/fraud-reviews/tasks/{taskId}/claim
POST /api/workflows/fraud-reviews/tasks/{taskId}/complete
```

Tạo các class chính:

```text
workflow-service/src/main/java/com/example/workflow/api/controller/FraudReviewController.java
workflow-service/src/main/java/com/example/workflow/application/service/FraudReviewService.java
workflow-service/src/test/java/com/example/workflow/application/service/FraudReviewProcessTests.java
```

Có thể tạo thêm request/response DTO riêng nếu cần.

## Luồng BPMN Đề Xuất

```text
Start Fraud Review
  -> Evaluate Fraud Risk bằng fraud-risk.dmn
  -> Gateway theo riskLevel
     -> LOW: Approve Review
     -> MEDIUM: Fraud Analyst Review user task
     -> HIGH: Fraud Manager Review user task
  -> Gateway theo approved
     -> approved=true: Review Approved
     -> approved=false: Review Rejected
```

User task đề xuất:

- `Fraud Analyst Review`, candidate group `fraud-analysts`.
- `Fraud Manager Review`, candidate group `fraud-managers`.

Mỗi user task cần có interrupting boundary timer, ví dụ:

```text
PT15M
```

Khi timer xảy ra:

- Set `approved=false`.
- Set `reviewStatus=TIMED_OUT`.
- Set `failureReason`.
- Kết thúc ở trạng thái từ chối.

## DMN Đề Xuất

Tên decision:

```text
fraud-risk
```

Input:

```text
fraudScore
orderAmount
```

Output:

```text
riskLevel
```

Quy tắc gợi ý:

| Điều kiện | riskLevel |
| --- | --- |
| `fraudScore < 40` | `LOW` |
| `fraudScore >= 40 and fraudScore < 80` | `MEDIUM` |
| `fraudScore >= 80` | `HIGH` |

## Process Variables

Sử dụng các biến sau:

```text
reviewId
orderId
customerId
orderAmount
fraudScore
riskLevel
approved
reviewStatus
failureReason
correlationId
```

Giá trị hợp lệ cho `riskLevel`:

```text
LOW
MEDIUM
HIGH
```

Giá trị hợp lệ cho `reviewStatus`:

```text
APPROVED
REJECTED
TIMED_OUT
```

## API Request Gợi Ý

Start fraud review:

```json
{
  "reviewId": "fraud-review-1001",
  "orderId": "order-1001",
  "customerId": "customer-42",
  "orderAmount": 1200.00,
  "fraudScore": 65,
  "correlationId": "correlation-1001"
}
```

Complete task:

```json
{
  "approved": true,
  "approver": "fraud-analyst-1",
  "comment": "Thông tin hợp lệ"
}
```

## Tiêu Chí Nghiệm Thu

- Process `fraud-review` deploy thành công cùng `workflow-service`.
- DMN `fraud-risk.dmn` deploy thành công.
- Với `riskLevel=LOW`, process tự động kết thúc với `reviewStatus=APPROVED`, không tạo user task.
- Với `riskLevel=MEDIUM`, process tạo user task cho group `fraud-analysts`.
- Với `riskLevel=HIGH`, process tạo user task cho group `fraud-managers`.
- API list task trả về `taskId`.
- API claim task gán được `assignee`.
- API complete task với `approved=true` đưa process tới trạng thái approved.
- API complete task với `approved=false` đưa process tới trạng thái rejected.
- Nếu quá hạn timer, process tự động set `approved=false`, `reviewStatus=TIMED_OUT`, và kết thúc ở nhánh rejected.
- Có unit test hoặc process test cho ít nhất các case: low risk, medium risk, high risk, reject thủ công, timeout.

## Validation Bắt Buộc

Chạy tối thiểu:

```bash
./mvnw -pl workflow-service test
```

Trước khi bàn giao branch, chạy:

```bash
./mvnw clean verify
```

## Branch Đề Xuất

```text
feature/fraud-review-workflow
```

## Lưu Ý Khi Làm

- Không sửa logic chính của `order-processing.bpmn`.
- Không dùng chung endpoint với approval task hiện có của order.
- Nếu cần thêm biến dùng chung, cân nhắc thêm vào `ProcessVariables`.
- Ưu tiên test process bằng Camunda engine thay vì chỉ test controller.
