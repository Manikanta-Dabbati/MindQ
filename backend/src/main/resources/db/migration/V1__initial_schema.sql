-- MindQ V1 Initial Schema
-- Created: 2026-08-19

-- =====================================================
-- USERS
-- =====================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- =====================================================
-- AI MODELS
-- =====================================================
CREATE TABLE ai_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model_code VARCHAR(100) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    max_tokens INT,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_ai_models_name UNIQUE (name),
    CONSTRAINT uk_ai_models_code UNIQUE (model_code)
);

-- =====================================================
-- STUDY MATERIALS
-- =====================================================
CREATE TABLE study_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    material_type VARCHAR(20) NOT NULL,
    raw_text LONGTEXT NOT NULL,
    file_name VARCHAR(255),
    file_size_bytes BIGINT,
    word_count INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_materials_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_materials_user ON study_materials(user_id);
CREATE INDEX idx_materials_created ON study_materials(created_at);

-- =====================================================
-- MCQ SETS
-- =====================================================
CREATE TABLE mcq_sets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    study_material_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    topic VARCHAR(150),
    difficulty VARCHAR(20) NOT NULL,
    total_questions INT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_mcqsets_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_mcqsets_material FOREIGN KEY (study_material_id) REFERENCES study_materials(id)
);
CREATE INDEX idx_mcqsets_user ON mcq_sets(user_id);
CREATE INDEX idx_mcqsets_material ON mcq_sets(study_material_id);
CREATE INDEX idx_mcqsets_created ON mcq_sets(created_at);

-- =====================================================
-- QUESTIONS
-- =====================================================
CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mcq_set_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    explanation TEXT,
    question_type VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    difficulty VARCHAR(20) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_questions_mcqset FOREIGN KEY (mcq_set_id) REFERENCES mcq_sets(id)
);
CREATE INDEX idx_questions_mcqset ON questions(mcq_set_id);

-- =====================================================
-- QUESTION OPTIONS
-- =====================================================
CREATE TABLE question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT NOT NULL,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id)
);
CREATE INDEX idx_options_question ON question_options(question_id);

-- =====================================================
-- QUIZ ATTEMPTS
-- =====================================================
CREATE TABLE quiz_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mcq_set_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    score INT NOT NULL DEFAULT 0,
    total_questions INT NOT NULL,
    percentage DECIMAL(5,2) DEFAULT 0,
    quiz_mode VARCHAR(20) NOT NULL DEFAULT 'PRACTICE',
    time_limit_minutes INT,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_attempts_mcqset FOREIGN KEY (mcq_set_id) REFERENCES mcq_sets(id)
);
CREATE INDEX idx_attempts_user ON quiz_attempts(user_id);
CREATE INDEX idx_attempts_mcqset ON quiz_attempts(mcq_set_id);
CREATE INDEX idx_attempts_started ON quiz_attempts(started_at);

-- =====================================================
-- QUIZ ANSWERS
-- =====================================================
CREATE TABLE quiz_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    time_taken_seconds INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_answers_attempt FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempts(id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_answers_option FOREIGN KEY (selected_option_id) REFERENCES question_options(id),
    CONSTRAINT uq_attempt_question UNIQUE (quiz_attempt_id, question_id)
);
CREATE INDEX idx_answers_attempt ON quiz_answers(quiz_attempt_id);

-- =====================================================
-- SAVED QUESTIONS
-- =====================================================
CREATE TABLE saved_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    notes TEXT,
    saved_at DATETIME NOT NULL,
    CONSTRAINT fk_saved_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_saved_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uq_user_question UNIQUE (user_id, question_id)
);
CREATE INDEX idx_saved_user ON saved_questions(user_id);

-- =====================================================
-- GENERATION HISTORY
-- =====================================================
CREATE TABLE generation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ai_model_id BIGINT NOT NULL,
    study_material_id BIGINT,
    mcq_set_id BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_history_model FOREIGN KEY (ai_model_id) REFERENCES ai_models(id),
    CONSTRAINT fk_history_material FOREIGN KEY (study_material_id) REFERENCES study_materials(id),
    CONSTRAINT fk_history_mcqset FOREIGN KEY (mcq_set_id) REFERENCES mcq_sets(id)
);
CREATE INDEX idx_history_user ON generation_history(user_id);
CREATE INDEX idx_history_created ON generation_history(created_at);

-- =====================================================
-- REFRESH TOKENS
-- =====================================================
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_refresh_token UNIQUE (token)
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);

-- =====================================================
-- FAILED LOGIN ATTEMPTS
-- =====================================================
CREATE TABLE failed_login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    attempted_at DATETIME NOT NULL
);
CREATE INDEX idx_failed_login_email ON failed_login_attempts(email);
CREATE INDEX idx_failed_login_timestamp ON failed_login_attempts(attempted_at);

-- =====================================================
-- PASSWORD RESET TOKENS
-- =====================================================
CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_reset_token UNIQUE (token)
);
CREATE INDEX idx_reset_user ON password_reset_tokens(user_id);
