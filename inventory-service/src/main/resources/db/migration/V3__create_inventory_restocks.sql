CREATE TABLE inventory_restocks (
  restock_id VARCHAR(64) PRIMARY KEY,
  return_id VARCHAR(64) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INTEGER NOT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  correlation_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_inventory_restock_return_sku
    UNIQUE (return_id, sku)
);