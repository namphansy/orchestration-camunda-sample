CREATE TABLE return_shipments (
  return_shipment_id VARCHAR(64) PRIMARY KEY,
  return_id VARCHAR(64) NOT NULL UNIQUE,
  order_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  correlation_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL
);