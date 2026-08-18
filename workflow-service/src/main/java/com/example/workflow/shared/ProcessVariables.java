package com.example.workflow.shared;

public final class ProcessVariables {

    public static final String ORDER_ID = "orderId";
    public static final String BUSINESS_KEY = "businessKey";
    public static final String CORRELATION_ID = "correlationId";
    public static final String ORDER_LINES = "orderLines";
    public static final String INVENTORY_RESERVATION_ID = "inventoryReservationId";
    public static final String INVENTORY_RESERVATION_IDS = "inventoryReservationIds";
    public static final String ORDER_STATUS = "orderStatus";
    public static final String INVENTORY_STATUS = "inventoryStatus";
    public static final String PAYMENT_STATUS = "paymentStatus";
    public static final String PAYMENT_TRANSACTION_ID = "paymentTransactionId";
    public static final String PAYMENT_CONFIRMED = "paymentConfirmed";
    public static final String PAYMENT_REFUND_STATUS = "paymentRefundStatus";
    public static final String ORDER_AMOUNT = "orderAmount";
    public static final String APPROVAL_LEVEL = "approvalLevel";
    public static final String APPROVED = "approved";
    public static final String APPROVER = "approver";
    public static final String INVOICE_ID = "invoiceId";
    public static final String INVOICE_STATUS = "invoiceStatus";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String NOTIFICATION_STATUS = "notificationStatus";
    public static final String SHIPMENT_ID = "shipmentId";
    public static final String SHIPMENT_STATUS = "shipmentStatus";
    public static final String INVENTORY_RELEASE_STATUS = "inventoryReleaseStatus";
    public static final String FAILURE_REASON = "failureReason";
    
    // Backorder variables
    public static final String BACKORDER_ID = "backorderId";
    public static final String SKU = "sku";
    public static final String QUANTITY = "quantity";
    public static final String BACKORDER_STATUS = "backorderStatus";
    public static final String RESTOCK_RECEIVED = "restockReceived";

    private ProcessVariables() {
    }
}
