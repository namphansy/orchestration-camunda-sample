ALTER TABLE payment_transactions
  ADD COLUMN refund_idempotency_key VARCHAR(128);

ALTER TABLE payment_transactions
  ADD COLUMN refunded_at TIMESTAMP;
