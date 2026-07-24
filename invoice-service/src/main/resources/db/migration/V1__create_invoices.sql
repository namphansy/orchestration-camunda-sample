CREATE TABLE invoices (
  invoice_id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  amount DECIMAL(19, 2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  correlation_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
