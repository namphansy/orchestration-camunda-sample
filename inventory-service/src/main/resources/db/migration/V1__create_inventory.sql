CREATE TABLE stock_items (
  sku VARCHAR(64) PRIMARY KEY,
  available_quantity INTEGER NOT NULL
);

CREATE TABLE inventory_reservations (
  reservation_id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INTEGER NOT NULL,
  status VARCHAR(32) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  correlation_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

INSERT INTO stock_items (sku, available_quantity) VALUES ('SKU-DEFAULT', 100);
