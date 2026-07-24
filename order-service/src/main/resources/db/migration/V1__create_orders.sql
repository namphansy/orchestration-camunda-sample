CREATE TABLE orders (
  order_id VARCHAR(64) PRIMARY KEY,
  customer_id VARCHAR(64) NOT NULL,
  order_amount DECIMAL(19, 2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INTEGER NOT NULL,
  status VARCHAR(32) NOT NULL,
  correlation_id VARCHAR(64) NOT NULL,
  workflow_process_instance_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
