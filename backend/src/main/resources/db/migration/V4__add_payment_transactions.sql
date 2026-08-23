-- MindQ V4 — Payment Transactions
-- Added: 2026-08-19

-- =====================================================
-- PAYMENT TRANSACTIONS
-- =====================================================
CREATE TABLE payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_order_id VARCHAR(100) NOT NULL,
    provider_payment_id VARCHAR(100),
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500),
    billing_period VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    valid_from DATETIME NOT NULL,
    valid_until DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_txn_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
    CONSTRAINT uk_txn_order UNIQUE (provider_order_id)
);
CREATE INDEX idx_txn_user ON payment_transactions(user_id);
CREATE INDEX idx_txn_status ON payment_transactions(status);
CREATE INDEX idx_txn_created ON payment_transactions(created_at);
