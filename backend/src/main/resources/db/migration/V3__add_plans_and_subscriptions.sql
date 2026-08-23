-- MindQ V3 — Plans & Subscriptions
-- Added: 2026-08-19

-- =====================================================
-- PLANS
-- =====================================================
CREATE TABLE plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    storage_limit_bytes BIGINT NOT NULL DEFAULT 524288000,
    daily_ai_generations INT NOT NULL DEFAULT 20,
    max_questions_per_generation INT NOT NULL DEFAULT 20,
    advanced_models BOOLEAN NOT NULL DEFAULT FALSE,
    ai_tutor BOOLEAN NOT NULL DEFAULT FALSE,
    export_formats BOOLEAN NOT NULL DEFAULT FALSE,
    priority_support BOOLEAN NOT NULL DEFAULT FALSE,
    price_in_paise INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_plans_code UNIQUE (code)
);

-- Seed plans
INSERT INTO plans (code, display_name, description, storage_limit_bytes, daily_ai_generations, max_questions_per_generation, advanced_models, ai_tutor, export_formats, priority_support, price_in_paise, created_at) VALUES
('FREE', 'Free', '500 MB storage, 20 AI generations/day, basic features', 524288000, 20, 20, FALSE, FALSE, FALSE, FALSE, 0, NOW()),
('PRO', 'Pro', '5 GB storage, 100 AI generations/day, advanced models, PDF export', 5368709120, 100, 20, TRUE, FALSE, TRUE, FALSE, 49900, NOW()),
('PREMIUM', 'Premium', '20 GB storage, unlimited AI, AI tutor, priority support', 21474836480, 999, 30, TRUE, TRUE, TRUE, TRUE, 99900, NOW());

-- =====================================================
-- USER SUBSCRIPTIONS
-- =====================================================
CREATE TABLE user_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATETIME NOT NULL,
    end_date DATETIME,
    cancelled_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);
CREATE INDEX idx_sub_user ON user_subscriptions(user_id);
CREATE INDEX idx_sub_status ON user_subscriptions(status);
