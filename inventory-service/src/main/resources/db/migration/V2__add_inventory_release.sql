ALTER TABLE inventory_reservations
  ADD COLUMN release_idempotency_key VARCHAR(128);

ALTER TABLE inventory_reservations
  ADD COLUMN released_at TIMESTAMP;
