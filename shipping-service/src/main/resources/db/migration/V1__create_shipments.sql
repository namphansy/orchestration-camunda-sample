CREATE TABLE shipments (
  shipment_id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INTEGER NOT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  correlation_id VARCHAR(64) NOT NULL,
  failure_reason VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  cancelled_at TIMESTAMP
);
