CREATE TABLE IF NOT EXISTS fraud_reviews (
    review_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    order_amount NUMERIC(19, 2) NOT NULL,
    fraud_score INT NOT NULL,
    risk_level VARCHAR(20),
    review_status VARCHAR(32) NOT NULL,
    assignee VARCHAR(64),
    approver VARCHAR(64),
    comment TEXT,
    failure_reason TEXT,
    process_instance_id VARCHAR(64),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fr_order_id ON fraud_reviews(order_id);
CREATE INDEX IF NOT EXISTS idx_fr_status ON fraud_reviews(review_status);
